package actors

import actors.TestablePersistentFSM.{GetInternals, Internals}
import akka.actor.ActorRef
import akka.persistence.fsm.PersistentFSM
import akka.persistence.fsm.PersistentFSM.FSMState
import akka.util.Timeout

import scala.concurrent.Future

trait TestablePersistentFSM[S <: FSMState, D, E] {

  _: PersistentFSM[S, D, E] =>

  whenUnhandled {
    case Event(GetInternals, data) =>
      sender ! Internals(stateName, data)
      stay
  }

}


object TestablePersistentFSM {

  import akka.pattern._
  import scala.concurrent.duration._

  implicit val timeout: Timeout = 10 second

  case object GetInternals

  case class Internals[S <: FSMState, D](state: S, data: D)

  def getInternalsOf[S <: FSMState, D](ref: ActorRef): Future[Internals[S, D]] = {
    (ref ? GetInternals).mapTo[Internals[S, D]]
  }

}
