package actors

import akka.actor.{Actor, Props}

/**
  * Created by mactur on 02/10/2016.
  */
class EaterActor extends Actor {

  override def receive: Receive = {
    case any => unhandled(any)
  }

}

object EaterActor {

  def props: Props = Props[EaterActor]

}