package actors

import actors.LunchbotActor.{MentionMessage, MessageBundle, OutboundMessage}
import akka.actor.{Actor, ActorRef, Props}
import akka.pattern._
import akka.util.Timeout
import commands.{CommandParsing, CommandUsage, Help}
import model.Statuses._
import model.{Statuses, UserId}
import slack.SlackUtil
import slack.models.Message
import slack.rtm.SlackRtmConnectionActor.SendMessage
import util.{Formatting, Logging}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Created by mactur on 29/09/2016.
  */
class LunchbotActor(selfId: String)
  extends Actor
    with Logging
    with Formatting
    with CommandParsing
    with CommandUsage {

  implicit val askTimeout: Timeout = Timeout(1 second)
  implicit val executionContext: ExecutionContext = context.dispatcher

  val lunchActor: ActorRef = context.actorOf(LunchActor.props, "lunch")

  override def receive: Receive = {

    case message: Message if SlackUtil.mentionsId(message.text, selfId) && message.user != selfId =>

      logger.debug(s"BOT IN: $message")

      val slack = sender()

      val textWithNoMentions = removeMentions(message.text).replaceAll(selfId, "")

      parse(message.copy(text = textWithNoMentions)) match {

        case Some(Help(_)) =>
          slack ! toSendMessage(message.channel, renderUsage(selfId), Success)

        case Some(command) =>
          (lunchActor ? command)
            .mapTo[OutboundMessage]
            .map(unbundle)
            .map(_.map { out => toSendMessage(message.channel, out) })
            .map(_.map { out => logger.debug(s"BOT OUT: $out"); out })
            .map(_.foreach(slack ! _))

        case None =>
          slack ! toSendMessage(message.channel, MentionMessage("I didn't quite get that...", message.user, Failure))

      }

  }

  private def unbundle(outboundMessage: OutboundMessage): Seq[OutboundMessage] = {
    outboundMessage match {
      case MessageBundle(outboundMessages) => outboundMessages
      case singleOutboundMessage => Seq(singleOutboundMessage)
    }
  }

  private def toSendMessage(channel: String, outboundMessage: OutboundMessage): SendMessage = {
    toSendMessage(channel, outboundMessage.getText, outboundMessage.status)
  }

  private def toSendMessage(channel: String, text: String, status: Status): SendMessage = {
    SendMessage(channel, s"${statusIcon(status)} $text")
  }

}

object LunchbotActor extends Formatting {

  def props(selfId: String): Props = Props(new LunchbotActor(selfId))

  sealed trait OutboundMessage {
    def getText: String

    val status: Status
  }

  case class HereMessage(text: String, status: Status) extends OutboundMessage {
    override def getText: String = s"<!here> $text"
  }

  case class MentionMessage(text: String, mentionedUser: UserId, status: Status) extends OutboundMessage {
    override def getText: String = s"${formatMention(mentionedUser)} $text"
  }

  case class SimpleMessage(text: String, status: Status) extends OutboundMessage {
    override def getText: String = text
  }

  case class MessageBundle(messages: Seq[OutboundMessage]) extends OutboundMessage {
    override def getText: String = messages.map(_.getText).mkString("\n")

    override val status: Status = Success
  }

}
