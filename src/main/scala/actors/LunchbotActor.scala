package actors

import com.cyberdolphins.slime._
import com.cyberdolphins.slime.SlackBotActor.{Close, Connect}
import com.cyberdolphins.slime.incoming._
import com.cyberdolphins.slime.outgoing._
import com.cyberdolphins.slime.common._

/**
  * Created by mactur on 29/09/2016.
  */
class LunchbotActor extends SlackBotActor {

  override def eventReceive: EventReceive = {

    case SimpleInboundMessage(text, channel, user) =>

      publish(s"Echo: $text", channel, user)

  }

}
