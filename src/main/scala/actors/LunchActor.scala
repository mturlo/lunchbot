package actors

import actors.EaterActor.{FoodChosen, FoodData, Joined, Paid}
import actors.LunchActor._
import actors.LunchbotActor.{HereMessage, MentionMessage, SimpleMessage}
import akka.actor.{ActorRef, FSM, Props}
import akka.pattern._
import akka.util.Timeout
import commands._
import model.UserId
import util.{Formatting, Logging}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

/**
  * Created by mactur on 01/10/2016.
  */
class LunchActor
  extends FSM[State, Data]
    with Logging
    with Formatting {

  implicit val askTimeout = Timeout(1 second)

  startWith(Idle, Empty)

  when(Idle) {

    case Event(Create(lunchmaster, place), _) =>
      val formattedPlace = formatUrl(place)
      sender ! HereMessage(s"Created new lunch instance at: $formattedPlace with ${formatMention(lunchmaster)} as Lunchmaster")
      goto(InProgress) using LunchData(lunchmaster, formattedPlace, Map.empty)

    case Event(_, _) =>
      sender ! SimpleMessage("No current running lunch processes")
      stay

  }

  when(InProgress) {

    case Event(Create(_, _), LunchData(lunchmaster, place, _)) =>
      sender ! SimpleMessage(s"There is already a running lunch process at: $place with ${formatMention(lunchmaster)} as Lunchmaster")
      stay

    case Event(Join(eaterId), currentData@LunchData(_, place, eaters)) =>
      eaters.get(eaterId) match {
        case Some(_) =>
          sender ! MentionMessage(s"You've already joined this lunch!", eaterId)
          stay using currentData
        case None =>
          sender ! MentionMessage(s"Successfully joined the lunch at $place", eaterId)
          stay using currentData.withEater(eaterId, context.actorOf(EaterActor.props(eaterId)))
      }

    case Event(choose@Choose(eaterId, _), LunchData(_, _, eaters)) =>
      eaters.get(eaterId) match {
        case Some(eaterActor) =>
          (eaterActor ? choose).pipeTo(sender)
        case None =>
          MentionMessage(s"You have to join this lunch first!", eaterId)
      }
      stay

    case Event(pay@Pay(eaterId), LunchData(_, _, eaters)) =>
      eaters.get(eaterId) match {
        case Some(eaterActor) =>
          (eaterActor ? pay).pipeTo(sender)
        case None =>
          MentionMessage(s"You have to join this lunch first!", eaterId)
      }
      stay

    case Event(Cancel(canceller), LunchData(lunchmaster, _, _)) =>
      if (canceller == lunchmaster) {
        sender ! SimpleMessage("Cancelled current lunch process")
        goto(Idle) using Empty
      } else {
        sender ! SimpleMessage("Only lunchmasters can cancel lunches!")
        stay
      }

    case Event(summary@Summary(_), LunchData(_, _, eaters)) =>
      val slack = sender
      Future.traverse(eaters.values) { eaterRef =>
        (eaterRef ? summary).mapTo[EaterReport]
      } map { reports =>
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
          case foods => s"The current order is:\n${foods.map(f => s"• $f").mkString("\n")}"
        }
        val summaryMessage = (stateMessages.toSeq :+ totalFoodsMessage).mkString("\n")
        slack ! SimpleMessage(summaryMessage)
      }
      stay

  }

  initialize

}

object LunchActor {

  sealed trait State

  case object Idle extends State

  case object InProgress extends State

  sealed trait Data

  case object Empty extends Data

  case class LunchData(lunchmaster: UserId, place: String, eaters: Map[UserId, ActorRef]) extends Data {
    def withEater(eaterId: UserId, eaterActor: ActorRef): LunchData = {
      copy(eaters = eaters + (eaterId -> eaterActor))
    }
  }

  case class EaterReport(eaterId: UserId, state: EaterActor.State, data: EaterActor.Data)

  def props: Props = Props[LunchActor]

}