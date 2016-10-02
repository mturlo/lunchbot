package actors

import actors.LunchbotActor.OutboundMessage
import akka.actor.{Actor, Props}
import commands.Create
import util.Logging

/**
  * Created by mactur on 01/10/2016.
  */
class LunchActor extends Actor with Logging {

  override def receive: Receive = {

    case Create(lunchmaster, place) =>
      sender ! OutboundMessage(s"Created new lunch instance at: $place with $lunchmaster as Lunchmaster")

  }

}

object LunchActor {

  def props: Props = Props[LunchActor]

}