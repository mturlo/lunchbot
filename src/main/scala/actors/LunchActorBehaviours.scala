package actors

import actors.EaterActor.{FoodChosen, FoodData, Joined, Paid}
import actors.LunchActor._
import actors.LunchbotActor._
import akka.actor.{ActorRef, FSM, PoisonPill}
import akka.pattern._
import commands._
import model.Statuses._
import util.Formatting

import scala.concurrent.Future.{apply => _, _}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
  * Created by mactur on 09/10/2016.
  */
trait LunchActorBehaviours {

  _: FSM[State, Data]
    with Formatting =>

  implicit val askTimeout: akka.util.Timeout
  implicit val executionContext: ExecutionContext

  trait WhenIdle {

    def create(command: Create, sender: ActorRef): State = {
      val formattedPlace = formatUrl(command.place)
      sender ! HereMessage(s"Created new lunch instance at: $formattedPlace with ${formatMention(command.lunchmaster)} as Lunchmaster", Success)
      goto(InProgress) using LunchData(command.lunchmaster, formattedPlace, Map.empty)
    }

    def unhandled(sender: ActorRef): State = {
      sender ! SimpleMessage("No current running lunch processes", Failure)
      stay
    }

  }

  object WhenIdle extends WhenIdle

  trait WhenInProgress {

    def create(command: Create, sender: ActorRef): State = {
      sender ! SimpleMessage(s"There is already a running lunch process at: $command.place with ${formatMention(command.lunchmaster)} as Lunchmaster", Failure)
      stay
    }

    def cancel(command: Cancel, data: LunchData, sender: ActorRef): State = {
      if (command.canceller == data.lunchmaster) {
        sender ! SimpleMessage("Cancelled current lunch process", Success)
        goto(Idle) using Empty
      } else {
        sender ! SimpleMessage("Only lunchmasters can cancel lunches!", Failure)
        stay
      }
    }

    def poke(command: Poke, data: LunchData, sender: ActorRef): State = {
      val slack = sender
      if (command.poker == data.lunchmaster) {
        fanIn[OutboundMessage](data.eaters.values.toSeq, command)
          .map(MessageBundle)
          .map(slack ! _)
      } else {
        slack ! SimpleMessage("Only lunchmasters can poke eaters!", Failure)
      }
      stay
    }

    def kick(command: Kick, data: LunchData, sender: ActorRef): State = {
      val slack = sender
      if (command.kicker == data.lunchmaster) {
        data.eaters.get(command.kicked) match {
          case Some(eater) =>
            sender ! SimpleMessage(s"Successfully kicked ${formatMention(command.kicked)} from the current lunch", Success)
            eater ! PoisonPill
            stay using data.removeEater(command.kicked)
          case None =>
            slack ! SimpleMessage("But he hasn't even joined the lunch yet!", Failure)
            stay
        }
      } else {
        slack ! SimpleMessage("Only lunchmasters can kick eaters!", Failure)
        stay
      }
    }

    def summary(command: Summary, data: LunchData, sender: ActorRef): State = {
      val slack = sender
      fanIn[EaterReport](data.eaters.values.toSeq, command)
        .map { reports =>
          val stateMessages = reports.groupBy(_.state) map {
            case (Joined, reportsByState) =>
              s"There are ${reportsByState.size} eaters who only joined the lunch"
            case (FoodChosen, reportsByState) =>
              s"There are ${reportsByState.size} eaters who chose their food"
            case (Paid, reportsByState) =>
              s"There are ${reportsByState.size} eaters who already paid for their food"
          }
          val totalFoods = reports.map(_.data).map {
            case FoodData(food) => Some(food)
            case _ => None
          }
          val totalFoodsMessage = totalFoods.flatten match {
            case Nil => "Nobody has chosen their food yet"
            case foods =>
              val foodsWithCounts = foods.map(f => (f, foods.count(_ == f))).distinct
              s"The current order is:\n${foodsWithCounts.map(f => s"• ${f._1} [x${f._2}]").mkString("\n")}"
          }
          val summaryMessage = (stateMessages.toSeq :+ totalFoodsMessage).mkString("\n")
          slack ! SimpleMessage(summaryMessage, Success)
        }
      stay
    }

    def join(command: Join, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.eater) match {
        case Some(_) =>
          sender ! MentionMessage(s"You've already joined this lunch!", command.eater, Failure)
          stay using data
        case None =>
          sender ! MentionMessage(s"Successfully joined the lunch at ${data.place}", command.eater, Success)
          stay using data.withEater(command.eater, context.actorOf(EaterActor.props(command.eater)))
      }
    }

    def leave(command: Leave, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.eater) match {
        case Some(eater) =>
          sender ! MentionMessage(s"Well, see you next time!", command.eater, Success)
          eater ! PoisonPill
          stay using data.removeEater(command.eater)
        case None =>
          sender ! MentionMessage(s"You were not going to eat at ${data.place} anyway", command.eater, Failure)
          stay using data
      }
    }

    def choose(command: Choose, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.eater) match {
        case Some(eaterActor) =>
          (eaterActor ? command).pipeTo(sender)
        case None =>
          sender ! MentionMessage(s"You have to join this lunch first!", command.eater, Failure)
      }
      stay
    }

    def pay(command: Pay, data: LunchData, sender: ActorRef): State = {
      data.eaters.get(command.payer) match {
        case Some(eaterActor) =>
          (eaterActor ? command).pipeTo(sender)
        case None =>
          MentionMessage(s"You have to join this lunch first!", command.payer, Failure)
      }
      stay
    }

    def close(command: Close, data: LunchData, sender: ActorRef): State = {
      if (command.closer == data.lunchmaster) {
        val currentState = stateName
        val joinedOnly = fanIn[EaterReport](data.eaters.values.toSeq, Summary(command.closer)) map { reports =>
          reports.filter(_.state == EaterActor.Joined)
        }
        joinedOnly map {
          case Nil =>
            sender ! SimpleMessage("Current lunch is now closed, can't join or change orders", Success)
            self ! Closed
          case _ =>
            sender ! SimpleMessage(s"Cannot close - some people didn't choose their food.", Failure)
            self ! currentState
        }
        goto(WaitingForState) using data
      } else {
        sender ! SimpleMessage("Only lunchmasters can close lunches!", Failure)
        stay
      }
    }

  }

  object WhenInProgress extends WhenInProgress

  trait WhenClosed extends WhenInProgress {

    override def close(command: Close, data: LunchData, sender: ActorRef): State = {
      if (command.closer == data.lunchmaster) {
        sender ! SimpleMessage("This lunch is already closed", Failure)
        stay
      } else {
        sender ! SimpleMessage("Only lunchmasters can close lunches!", Failure)
        stay
      }
    }

    def unhandled(sender: ActorRef): State = {
      sender ! SimpleMessage("Current lunch is now closed, can't change its state", Failure)
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

}
