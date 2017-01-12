package actors

import actors.EaterActor.{FoodChosen, FoodData, Joined, Paid}
import actors.LunchActor._
import actors.LunchbotActor._
import akka.actor.{ActorRef, FSM, PoisonPill}
import akka.pattern._
import commands._
import model.Statuses._
import modules.{Configuration, Messages}
import util.Formatting

import scala.concurrent.Future._
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

trait LunchActorBehaviours {

  _: FSM[State, Data]
    with Formatting
    with Configuration
    with Messages =>

  implicit val askTimeout: akka.util.Timeout
  implicit val executionContext: ExecutionContext

  trait WhenIdle {

    def create(command: Create, sender: ActorRef): State = {
      val formattedPlace = formatUrl(command.place)
      val message = messages[Create].created(formattedPlace, formatMention(command.caller))
      sender ! HereMessage(message, Success)
      goto(InProgress) using LunchData(command.caller, formattedPlace, Map.empty)
    }

    def unhandled(sender: ActorRef): State = {
      sender ! SimpleMessage(messages[Unhandled].noLunch, Failure)
      stay
    }

  }

  object WhenIdle extends WhenIdle

  trait WhenInProgress {

    def create(command: Create, data: LunchData, sender: ActorRef): State = {
      sender ! SimpleMessage(messages[Create].alreadyRunning(formatUrl(data.place), formatMention(data.lunchmaster)), Failure)
      stay
    }

    def finish(command: Finish, data: LunchData, sender: ActorRef): State = {
      lunchmasterOnly(command, data) {
        data.eaters.values foreach (_ ! PoisonPill)
        sender ! SimpleMessage(messages[Finish].finished, Success)
        goto(Idle) using Empty
      }
    }

    def poke(command: Poke, data: LunchData, sender: ActorRef): State = {
      val slack = sender
      lunchmasterOnly(command, data) {
        fanIn[OutboundMessage](data.eaters.values.toSeq, command)
          .map(MessageBundle)
          .map(slack ! _)
        stay
      }
    }

    def kick(command: Kick, data: LunchData, sender: ActorRef): State = {
      lunchmasterOnly(command, data) {
        data.eaters.get(command.kicked) match {
          case Some(eater) =>
            sender ! SimpleMessage(messages[Kick].kicked(formatMention(command.kicked)), Success)
            eater ! PoisonPill
            stay using data.removeEater(command.kicked)
          case None =>
            sender ! SimpleMessage(messages[Kick].notJoined, Failure)
            stay
        }
      }
    }

    def summary(command: Summary, data: LunchData, sender: ActorRef): State = {
      val slack = sender
      val headerMessage = messages[Summary].header(data.place, formatMention(data.lunchmaster))
      fanIn[EaterReport](data.eaters.values.toSeq, command)
        .map { reports =>
          val stateMessages = reports.groupBy(_.state) map {
            case (Joined, reportsByState) =>
              messages[Summary].joined(reportsByState.size)
            case (FoodChosen, reportsByState) =>
              messages[Summary].chosen(reportsByState.size)
            case (Paid, reportsByState) =>
              messages[Summary].paid(reportsByState.size)
          }
          val totalFoods = reports.map(_.data).map {
            case FoodData(food) => Some(food)
            case _ => None
          }
          val totalFoodsMessage = totalFoods.flatten match {
            case Nil => messages[Summary].nobodyChosen
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

    def join(command: Join, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.caller) match {
        case Some(_) =>
          sender ! MentionMessage(messages[Join].alreadyJoined, command.caller, Failure)
          stay using data
        case None =>
          sender ! ReactionMessage(Success)
          stay using data.withEater(command.caller, context.actorOf(EaterActor.props(command.caller, config), command.caller))
      }
    }

    def leave(command: Leave, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.caller) match {
        case Some(eater) =>
          sender ! ReactionMessage(Success)
          eater ! PoisonPill
          stay using data.removeEater(command.caller)
        case None =>
          sender ! MentionMessage(messages[Leave].notJoined(data.place), command.caller, Failure)
          stay using data
      }
    }

    def choose(command: Choose, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.caller) match {
        case Some(eaterActor) =>
          (eaterActor ? command).pipeTo(sender)
        case None =>
          sender ! MentionMessage(messages[Choose].notJoined, command.caller, Failure)
      }
      stay
    }

    def pay(command: Pay, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.caller) match {
        case Some(eaterActor) =>
          (eaterActor ? command).pipeTo(sender)
        case None =>
          MentionMessage(messages[Pay].notJoined, command.caller, Failure)
      }
      stay
    }

    def close(command: Close, data: LunchData, sender: ActorRef): State = {
      lunchmasterOnly(command, data) {
        val currentState = stateName
        val joinedOnly = fanIn[EaterReport](data.eaters.values.toSeq, Summary(command.caller)) map { reports =>
          reports.filter(_.state == EaterActor.Joined)
        }
        joinedOnly map {
          case Nil =>
            sender ! HereMessage(messages[Close].closed, Success)
            self ! Closed
          case _ =>
            sender ! SimpleMessage(messages[Close].someNotChosen, Failure)
            self ! currentState
        }
        goto(WaitingForState) using data
      }
    }

    def open(command: Open, data: LunchData, sender: ActorRef): State = {
      lunchmasterOnly(command, data) {
        sender ! SimpleMessage(messages[Open].alreadyOpen, Failure)
        stay
      }
    }

  }

  object WhenInProgress extends WhenInProgress

  trait WhenClosed extends WhenInProgress {

    override def close(command: Close, data: LunchData, sender: ActorRef): State = {
      lunchmasterOnly(command, data) {
        sender ! SimpleMessage(messages[Close].alreadyClosed, Failure)
        stay
      }
    }

    override def open(command: Open, data: LunchData, sender: ActorRef): State = {
      lunchmasterOnly(command, data) {
        sender ! SimpleMessage(messages[Open].opened(data.place), Success)
        goto(InProgress)
      }
    }

    override def poke(command: Poke, data: LunchData, sender: ActorRef): State = {
      val slack = sender
      lunchmasterOnly(command, data) {
        fanIn[OutboundMessage](data.eaters.values.toSeq, Poke.Pay(command.caller))
          .map(MessageBundle)
          .map(slack ! _)
        stay
      }
    }

    def unhandled(sender: ActorRef): State = {
      sender ! SimpleMessage(messages[Unhandled].closed, Failure)
      stay
    }

  }

  object WhenClosed extends WhenClosed

  private def fanIn[T](actors: Seq[ActorRef], command: Command)(implicit tag: ClassTag[T]): Future[Seq[T]] = {
    sequence(
      actors
        .map(_ ? command)
        .map(
          _.mapTo[T]
            .map(Some(_))
            .recover(PartialFunction(_ => None))
        )
    ).map(_.flatten)
  }

  private def lunchmasterOnly(command: Command, data: LunchData)(authorised: => State): State = {
    if (command.caller == data.lunchmaster) {
      authorised
    } else {
      sender ! SimpleMessage(messages[Unhandled].lunchmasterOnly, Failure)
      stay
    }
  }

}
