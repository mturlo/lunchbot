package actors

import actors.LunchbotActor.OutboundMessage
import akka.actor.{Actor, Props}
import commands.Create
import util.{Formatting, Logging}

/**
  * Created by mactur on 01/10/2016.
  */
class LunchActor
  extends Actor
    with Logging
    with Formatting {

  override def receive: Receive = {

    case Create(lunchmaster, place) =>
      sender ! OutboundMessage(s"Created new lunch instance at: ${formatUrl(place)} with ${formatMention(lunchmaster)} as Lunchmaster")

  }

}

object LunchActor {

  def props: Props = Props[LunchActor]

}