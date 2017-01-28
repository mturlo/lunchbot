package actors

import actors.EaterActor.{FoodChosen, FoodData, Joined, Paid}
import actors.LunchActor._
import actors.LunchbotActor._
import akka.actor.{PoisonPill, Props}
import akka.pattern._
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import akka.util.Timeout
import commands._
import model.Statuses.{Failure, Success}
import model.UserId
import service.MessagesService
import util.{Formatting, Logging}

import scala.concurrent.Future.sequence
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect._

class LunchActor(val messagesService: MessagesService)
  extends PersistentFSM[State, Data, LunchEvent]
    with TestablePersistentFSM[State, Data, LunchEvent]
    with Logging
    with Formatting {

  import messagesService._

  implicit val askTimeout = Timeout(100 milliseconds)
  implicit val executionContext: ExecutionContext = context.dispatcher

  startWith(Idle, Empty)

  when(Idle) {

    case Event(Create(caller, place), Empty) =>
      val formattedPlace = formatUrl(place)
      goto(InProgress) applying LunchCreated(caller, place) andThen { _ =>
        val message = messages[Create].created(formattedPlace, formatMention(caller))
        sender ! HereMessage(message, Success)
      }

    case Event(_ : Command, _) =>
      sender ! SimpleMessage(messages[Unhandled].noLunch, Failure)
      stay

  }

  val inProgress: StateFunction = {

    case Event(Create(_, _), data: LunchData) =>
      sender ! SimpleMessage(messages[Create].alreadyRunning(formatUrl(data.place), formatMention(data.lunchmaster)), Failure)
      stay

    case Event(Pay(caller), _) =>
      context.child(caller) match {
        case Some(_) =>
          stay applying EaterPaid(caller)
        case None             =>
          MentionMessage(messages[Pay].notJoined, caller, Failure)
          stay
      }

    case Event(Finish(caller), data: LunchData) =>
      lunchmasterOnly(caller, data) {
        goto(Idle) applying LunchFinished andThen { _ =>
          sender ! SimpleMessage(messages[Finish].finished, Success)
        }
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
          stay
        } else {
          stay applying EaterAdded(caller) andThen { _ =>
            sender ! ReactionMessage(Success)
          }
        }

      case Event(Leave(caller), data: LunchData) =>
        if (data.eaters.contains(caller)) {
          stay applying EaterRemoved(caller) andThen { _ =>
            sender ! ReactionMessage(Success)
          }
        } else {
          sender ! MentionMessage(messages[Leave].notJoined(data.place), caller, Failure)
          stay
        }

      case Event(command @ Choose(caller, food), _) =>
        context.child(caller) match {
          case Some(eaterActor) =>
            stay applying EaterFoodChosen(caller, food) andThen { _ =>
              (eaterActor ? command).pipeTo(sender)
            }
          case None             =>
            sender ! MentionMessage(messages[Choose].notJoined, caller, Failure)
            stay
        }

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
          goto(WaitingForState)
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
            stay applying EaterRemoved(kicked) andThen { _ =>
              sender ! SimpleMessage(messages[Kick].kicked(formatMention(kicked)), Success)
            }
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

      case Event(_ : Command, _) =>
        sender ! SimpleMessage(messages[Unhandled].closed, Failure)
        stay

    }

  }

  when(WaitingForState) {

    case Event(state: LunchActor.State, _) =>
      logger.debug(s"Changing state to received: $state")
      goto(state)

  }

  // akka persistence stuff

  override def domainEventClassTag: ClassTag[LunchEvent] = classTag[LunchEvent]

  override def persistenceId: String = getClass.getSimpleName

  override def applyEvent(domainEvent: LunchEvent, currentData: Data): Data = {
    logger.debug(s"applying event $domainEvent to data: $currentData")
    domainEvent match {
      case LunchCreated(lunchmaster, place) =>
        val formattedPlace = formatUrl(place)
        LunchData(lunchmaster, formattedPlace, Nil)
      case LunchFinished                    =>
        currentData.eaters.flatMap(context.child).foreach(_ ! PoisonPill)
        Empty
      case EaterAdded(eater)                =>
        context.actorOf(EaterActor.props(eater, messagesService), eater)
        currentData.withEater(eater)
      case EaterRemoved(eater)              =>
        context.child(eater).foreach(_ ! PoisonPill)
        currentData.removeEater(eater)
      case EaterFoodChosen(eater, food)     =>
        context.child(eater) foreach (_ ! Choose(eater, food))
        currentData
      case EaterPaid(eater)                 =>
        context.child(eater) foreach { eaterActor =>
          (eaterActor ? Pay(eater)).pipeTo(sender)
        }
        currentData
    }
  }

  // utils

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

  sealed trait State extends FSMState

  case object Idle extends State {
    override def identifier: String = "Idle"
  }

  case object InProgress extends State {
    override def identifier: String = "InProgress"
  }

  case object Closed extends State {
    override def identifier: String = "Closed"
  }

  case object WaitingForState extends State {
    override def identifier: String = "WaitingForState"
  }

  sealed trait Data {

    def eaters: Seq[UserId]

    def withEater(eaterId: UserId): Data

    def removeEater(eaterId: UserId): Data

  }

  case object Empty extends Data {

    override def eaters: Seq[UserId] = Nil

    override def withEater(eaterId: UserId): Data = Empty

    override def removeEater(eaterId: UserId): Data = Empty

  }

  case class LunchData(lunchmaster: UserId, place: String, eaters: Seq[UserId]) extends Data {

    def withEater(eaterId: UserId): Data = {
      copy(eaters = eaters :+ eaterId)
    }

    def removeEater(eaterId: UserId): Data = {
      copy(eaters = eaters.filterNot(_ == eaterId))
    }

  }

  sealed trait LunchEvent

  case class LunchCreated(lunchmaster: UserId, place: String) extends LunchEvent

  case object LunchFinished extends LunchEvent

  case class EaterAdded(eater: UserId) extends LunchEvent

  case class EaterRemoved(eater: UserId) extends LunchEvent

  case class EaterFoodChosen(eater: UserId, food: String) extends LunchEvent

  case class EaterPaid(eater: UserId) extends LunchEvent


  case class EaterReport(eaterId: UserId, state: EaterActor.State, data: EaterActor.Data)

  def props(messagesService: MessagesService): Props = Props(new LunchActor(messagesService))

}
