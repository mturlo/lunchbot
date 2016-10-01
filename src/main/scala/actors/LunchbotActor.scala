package actors

import com.cyberdolphins.slime._
import com.cyberdolphins.slime.SlackBotActor.{Close, Connect}
import com.cyberdolphins.slime.incoming._
import com.cyberdolphins.slime.outgoing._
import com.cyberdolphins.slime.common._
import play.api.Logger

/**
  * Created by mactur on 29/09/2016.
  */
class LunchbotActor extends SlackBotActor {

  val logger: Logger = Logger(getClass)

  val triggers: Set[String] = Set("lunchbot", "lb")

  val triggersRegex: String = triggers.map(tr => s"\\b$tr\\b").mkString("|")

  logger.debug(s"triggersRegex is $triggersRegex")

  val regex = s"($triggersRegex)(.*)"

  logger.debug(s"regex is $regex")

  override def eventReceive: EventReceive = {

    case SimpleInboundMessage(text, channel, user) if text.matches(regex) =>

      publish(s"""Hello, $user, I can hear you say: *$text*""", channel, user)

  }

}
