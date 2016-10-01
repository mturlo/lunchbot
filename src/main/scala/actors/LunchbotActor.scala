package actors

import akka.actor.{Actor, Props}
import slack.SlackUtil
import slack.models.Message
import slack.rtm.SlackRtmConnectionActor.SendMessage
import util.Logging

/**
  * Created by mactur on 29/09/2016.
  */
class LunchbotActor(selfId: String) extends Actor with Logging {

  override def receive: Receive = {

    case message: Message if SlackUtil.mentionsId(message.text, selfId) =>

      sender ! SendMessage(message.channel, s"<@${message.user}>: Hey!")

  }

}

object LunchbotActor {

  def props(selfId: String): Props = Props(new LunchbotActor(selfId))

}