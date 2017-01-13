package actors

import actors.LunchActor._
import akka.actor.{ActorRef, FSM, Props}
import akka.util.Timeout
import commands._
import model.UserId
import service.MessagesService
import util.{Formatting, Logging}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class LunchActor(val messagesService: MessagesService)
  extends FSM[State, Data]
    with LunchActorBehaviours
    with Logging
    with Formatting {

  implicit val askTimeout = Timeout(100 milliseconds)
  implicit val executionContext: ExecutionContext = context.dispatcher

  startWith(Idle, Empty)

  when(Idle) {

    case Event(command: Create, Empty) =>
      WhenIdle.create(command, sender)

    case Event(_, _) =>
      WhenIdle.unhandled(sender)

  }

  when(InProgress) {

    case Event(command: Create, lunchData: LunchData) =>
      WhenInProgress.create(command, lunchData, sender)

    case Event(command: Join, lunchData: LunchData) =>
      WhenInProgress.join(command, lunchData, sender)

    case Event(command: Leave, lunchData: LunchData) =>
      WhenInProgress.leave(command, lunchData, sender)

    case Event(command: Choose, lunchData: LunchData) =>
      WhenInProgress.choose(command, lunchData, sender)

    case Event(command: Pay, lunchData: LunchData) =>
      WhenInProgress.pay(command, lunchData, sender)

    case Event(command: Finish, lunchData: LunchData) =>
      WhenInProgress.finish(command, lunchData, sender)

    case Event(command: Close, lunchData: LunchData) =>
      WhenInProgress.close(command, lunchData, sender)

    case Event(command: Open, lunchData: LunchData) =>
      WhenInProgress.open(command, lunchData, sender)

    case Event(command: Poke, lunchData: LunchData) =>
      WhenInProgress.poke(command, lunchData, sender)

    case Event(command: Kick, lunchData: LunchData) =>
      WhenInProgress.kick(command, lunchData, sender)

    case Event(command: Summary, lunchData: LunchData) =>
      WhenInProgress.summary(command, lunchData, sender)

  }

  when(Closed) {

    case Event(command: Create, lunchData: LunchData) =>
      WhenClosed.create(command, lunchData, sender)

    case Event(command: Finish, lunchData: LunchData) =>
      WhenClosed.finish(command, lunchData, sender)

    case Event(command: Close, lunchData: LunchData) =>
      WhenClosed.close(command, lunchData, sender)

    case Event(command: Open, lunchData: LunchData) =>
      WhenClosed.open(command, lunchData, sender)

    case Event(command: Poke, lunchData: LunchData) =>
      WhenClosed.poke(command, lunchData, sender)

    case Event(command: Kick, lunchData: LunchData) =>
      WhenClosed.kick(command, lunchData, sender)

    case Event(command: Pay, lunchData: LunchData) =>
      WhenClosed.pay(command, lunchData, sender)

    case Event(command: Summary, lunchData: LunchData) =>
      WhenClosed.summary(command, lunchData, sender)

    case Event(_, _) =>
      WhenClosed.unhandled(sender)

  }

  when(WaitingForState) {

    case Event(state: LunchActor.State, _) =>
      logger.debug(s"Changing state to received: $state")
      goto(state)

  }

  initialize

}

object LunchActor {

  sealed trait State

  case object Idle extends State

  case object InProgress extends State

  case object Closed extends State

  case object WaitingForState extends State

  sealed trait Data

  case object Empty extends Data

  case class LunchData(lunchmaster: UserId, place: String, eaters: Map[UserId, ActorRef]) extends Data {

    def withEater(eaterId: UserId, eaterActor: ActorRef): LunchData = {
      copy(eaters = eaters + (eaterId -> eaterActor))
    }

    def removeEater(eaterId: UserId): LunchData = {
      copy(eaters = eaters - eaterId)
    }

  }

  case class EaterReport(eaterId: UserId, state: EaterActor.State, data: EaterActor.Data)

  def props(messagesService: MessagesService): Props = Props(new LunchActor(messagesService))

}
