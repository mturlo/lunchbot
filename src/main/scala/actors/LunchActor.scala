package actors

import actors.LunchActor._
import actors.LunchbotActor.OutboundMessage
import akka.actor.{FSM, Props}
import commands.{Cancel, Create}
import model.UserId
import util.{Formatting, Logging}

/**
  * Created by mactur on 01/10/2016.
  */
class LunchActor
  extends FSM[State, Data]
    with Logging
    with Formatting {

  startWith(Idle, Empty)

  when(Idle) {
    case Event(Create(lunchmaster, place), _) =>
      sender ! OutboundMessage(s"Created new lunch instance at: ${formatUrl(place)} with ${formatMention(lunchmaster)} as Lunchmaster")
      goto(InProgress).using(LunchData(lunchmaster, place, Seq.empty))
    case Event(Cancel, _) =>
      sender ! OutboundMessage("No current running lunch processes")
      stay
  }

  when(InProgress) {
    case Event(Create(_, _), LunchData(lunchmaster, place, _)) =>
      sender ! OutboundMessage(s"There is already a running lunch process at: ${formatUrl(place)} with ${formatMention(lunchmaster)} as Lunchmaster")
      stay
    case Event(Cancel, _) =>
      sender ! OutboundMessage("Cancelled current lunch process")
      goto(Idle).using(Empty)
  }

  initialize()

}

object LunchActor {

  sealed trait State

  case object Idle extends State

  case object InProgress extends State

  sealed trait Data

  case object Empty extends Data

  case class LunchData(lunchmaster: UserId, place: String, eaters: Seq[UserId]) extends Data

  def props: Props = Props[LunchActor]

}