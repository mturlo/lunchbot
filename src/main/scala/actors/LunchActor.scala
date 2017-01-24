package actors

import actors.EaterActor.{FoodChosen, FoodData, Joined, Paid}
import actors.LunchActor._
import actors.LunchbotActor._
import akka.actor.{FSM, PoisonPill, Props}
import akka.pattern._
import akka.util.Timeout
import commands._
import model.Statuses.{Failure, Success}
import model.UserId
import service.MessagesService
import util.{Formatting, Logging}

import scala.concurrent.Future.sequence
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

class LunchActor(val messagesService: MessagesService)
  extends FSM[State, Data]
    with Logging
    with Formatting {

  import messagesService._

  implicit val askTimeout = Timeout(100 milliseconds)
  implicit val executionContext: ExecutionContext = context.dispatcher

  startWith(Idle, Empty)

  when(Idle) {

    case Event(Create(caller, place), Empty) =>
      val formattedPlace = formatUrl(place)
      val message = messages[Create].created(formattedPlace, formatMention(caller))
      sender ! HereMessage(message, Success)
      goto(InProgress) using LunchData(caller, formattedPlace, Nil)

    case Event(_, _) =>
      sender ! SimpleMessage(messages[Unhandled].noLunch, Failure)
      stay

  }

  val inProgress: StateFunction = {

    case Event(Create(_, _), data: LunchData) =>
      sender ! SimpleMessage(messages[Create].alreadyRunning(formatUrl(data.place), formatMention(data.lunchmaster)), Failure)
      stay

    case Event(command @ Pay(caller), _) =>
      context.child(caller) match {
        case Some(eaterActor) =>
          (eaterActor ? command).pipeTo(sender)
        case None             =>
          MentionMessage(messages[Pay].notJoined, caller, Failure)
      }
      stay

    case Event(Finish(caller), data: LunchData) =>
      lunchmasterOnly(caller, data) {
        data.eaters.flatMap(context.child).foreach(_ ! PoisonPill)
        sender ! SimpleMessage(messages[Finish].finished, Success)
        goto(Idle) using Empty
      }

    case Event(command: Summary, data: LunchData) =>
      val slack = sender
      val headerMessage = messages[Summary].header(data.place, formatMention(data.lunchmaster))
      fanIn[EaterReport](data.eaters, command)
        .map { reports =>
          val stateMessages = reports.groupBy(_.state) map {
            case (Joined, reportsByState)     =>
              messages[Summary].joined(reportsByState.size)
            case (FoodChosen, reportsByState) =>
              messages[Summary].chosen(reportsByState.size)
            case (Paid, reportsByState)       =>
              messages[Summary].paid(reportsByState.size)
          }
          val totalFoods = reports.map(_.data).map {
            case FoodData(food) => Some(food)
            case _              => None
          }
          val totalFoodsMessage = totalFoods.flatten match {
            case Nil   => messages[Summary].nobodyChosen
            case foods =>
              val foodsWithCounts = foods.map(f => (f, foods.count(_ == f))).distinct
              val orderContents = foodsWithCounts.map(f => s"• ${f._1} [x${f._2}]").mkString("\n")
              messages[Summary].currentOrder(orderContents)
          }
          val summaryMessage = (headerMessage +: stateMessages.toSeq :+ totalFoodsMessage).mkString("\n")
          slack ! SimpleMessage(summaryMessage, Success)
        }
      stay

  }

  when(InProgress) {

    inProgress orElse {

      case Event(Join(caller), data: LunchData) =>
        if (data.eaters.contains(caller)) {
          sender ! MentionMessage(messages[Join].alreadyJoined, caller, Failure)
          stay using data
        } else {
          sender ! ReactionMessage(Success)
          context.actorOf(EaterActor.props(caller, messagesService), caller)
          stay using data.withEater(caller)
        }

      case Event(Leave(caller), data: LunchData) =>
        if (data.eaters.contains(caller)) {
          sender ! ReactionMessage(Success)
          context.child(caller).foreach(_ ! PoisonPill)
          stay using data.removeEater(caller)
        } else {
          sender ! MentionMessage(messages[Leave].notJoined(data.place), caller, Failure)
          stay using data
        }

      case Event(command @ Choose(caller, _), _) =>
        context.child(caller) match {
          case Some(eaterActor) =>
            (eaterActor ? command).pipeTo(sender)
          case None             =>
            sender ! MentionMessage(messages[Choose].notJoined, caller, Failure)
        }
        stay

      case Event(Close(caller), data: LunchData) =>
        val slack = sender()
        lunchmasterOnly(caller, data) {
          val currentState = stateName
          val joinedOnly = fanIn[EaterReport](data.eaters, Summary(caller)) map { reports =>
            reports.filter(_.state == EaterActor.Joined)
          }
          joinedOnly map {
            case Nil =>
              slack ! HereMessage(messages[Close].closed, Success)
              self ! Closed
            case _   =>
              slack ! SimpleMessage(messages[Close].someNotChosen, Failure)
              self ! currentState
          }
          goto(WaitingForState) using data
        }

      case Event(Open(caller), data: LunchData) =>
        lunchmasterOnly(caller, data) {
          sender ! SimpleMessage(messages[Open].alreadyOpen, Failure)
          stay
        }

      case Event(command @ Poke(caller), data: LunchData) =>
        lunchmasterOnly(caller, data) {
          val slack = sender
          fanIn[OutboundMessage](data.eaters, command)
            .map(MessageBundle)
            .map(slack ! _)
          stay
        }

      case Event(Kick(caller, kicked), data: LunchData) =>
        lunchmasterOnly(caller, data) {
          if (data.eaters.contains(kicked)) {
            sender ! SimpleMessage(messages[Kick].kicked(formatMention(kicked)), Success)
            context.child(kicked).foreach(_ ! PoisonPill)
            stay using data.removeEater(kicked)
          } else {
            sender ! SimpleMessage(messages[Kick].notJoined, Failure)
            stay

          }
        }

    }

  }

  when(Closed) {

    inProgress orElse {

      case Event(Close(caller), data: LunchData) =>
        lunchmasterOnly(caller, data) {
          sender ! SimpleMessage(messages[Close].alreadyClosed, Failure)
          stay
        }

      case Event(Open(caller), data: LunchData) =>
        lunchmasterOnly(caller, data) {
          sender ! SimpleMessage(messages[Open].opened(data.place), Success)
          goto(InProgress)
        }

      case Event(Poke(caller), data: LunchData) =>
        lunchmasterOnly(caller, data) {
          val slack = sender
          fanIn[OutboundMessage](data.eaters, Poke.Pay(caller))
            .map(MessageBundle)
            .map(slack ! _)
          stay
        }

      case Event(_, _) =>
        sender ! SimpleMessage(messages[Unhandled].closed, Failure)
        stay

    }

  }

  when(WaitingForState) {

    case Event(state: LunchActor.State, _) =>
      logger.debug(s"Changing state to received: $state")
      goto(state)

  }

  initialize

  private def fanIn[T](actorNames: Seq[String], command: Command)(implicit tag: ClassTag[T]): Future[Seq[T]] = {
    val actors = actorNames.flatMap(context.child)
    sequence(
      actors
        .map(_ ? command)
        .map(
          _.mapTo[T]
            .map(Some(_))
            .recover(PartialFunction(_ => None))
        )
    ).map(_.flatten.toSeq)
  }

  private def lunchmasterOnly(caller: UserId, data: LunchData)(authorised: => State): State = {
    if (caller == data.lunchmaster) {
      authorised
    } else {
      sender ! SimpleMessage(messages[Unhandled].lunchmasterOnly, Failure)
      stay
    }
  }

}

object LunchActor {

  sealed trait State

  case object Idle extends State

  case object InProgress extends State

  case object Closed extends State

  case object WaitingForState extends State

  sealed trait Data

  case object Empty extends Data

  case class LunchData(lunchmaster: UserId, place: String, eaters: Seq[UserId]) extends Data {

    def withEater(eaterId: UserId): LunchData = {
      copy(eaters = eaters :+ eaterId)
    }

    def removeEater(eaterId: UserId): LunchData = {
      copy(eaters = eaters.filterNot(_ == eaterId))
    }

  }

  case class EaterReport(eaterId: UserId, state: EaterActor.State, data: EaterActor.Data)

  def props(messagesService: MessagesService): Props = Props(new LunchActor(messagesService))

}
