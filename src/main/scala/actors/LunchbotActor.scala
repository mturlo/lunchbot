package actors

import actors.LunchbotActor.{MessageBundle, OutboundMessage, ReactionMessage, SimpleMessage}
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.Config
import commands._
import model.Statuses._
import model.UserId
import net.ceedubs.ficus.Ficus._
import service.{MessagesService, StatisticsService}
import slack.SlackUtil
import slack.api.BlockingSlackApiClient
import slack.models.Message
import slack.rtm.SlackRtmClient
import util.{Formatting, Logging}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class LunchbotActor(selfId: String,
                    messagesService: MessagesService,
                    statisticsService: StatisticsService,
                    slackRtmClient: SlackRtmClient,
                    slackApiClient: BlockingSlackApiClient,
                    config: Config)
  extends Actor
    with Logging
    with Formatting
    with CommandParsing
    with CommandUsage {

  implicit val askTimeout: Timeout = Timeout(1 second)
  implicit val executionContext: ExecutionContext = context.dispatcher
  implicit val actorSystem: ActorSystem = context.system

  val lunchActor: ActorRef = context.actorOf(LunchActor.props(messagesService), "lunch")

  val unrecognisedMsgs: List[String] = config.as[List[String]]("messages.unrecognised")

  override def receive: Receive = {

    case message: Message if SlackUtil.mentionsId(message.text, selfId) && message.user != selfId =>

      logger.debug(s"BOT IN: $message")

      val textWithNoMentions = removeMentions(message.text).replaceAll(selfId, "")

      parse(message.copy(text = textWithNoMentions)) match {

        case Some(Help(_)) =>
          sendMessage(message.channel, renderUsage(selfId), Success)

        case Some(Stats(_)) =>
          statisticsService.getLunchmasterStatistics.map { occurrenceMap =>
            val sorted = occurrenceMap.toSeq.sortBy(_._2).reverse
            val occurrenceString = sorted.map {
              case (master, count) => s"• ${formatMention(master)} [$count]"
            }.mkString("\n")
            s"Lunchmaster statistics:\n$occurrenceString"
          }.map { statisticsString =>
            sendMessage(message.channel, statisticsString, Success)
          }

        case Some(command) =>
          (lunchActor ? command)
            .mapTo[OutboundMessage]
            .map(unbundle)
            .map(_.map { out => logger.debug(s"BOT OUT: $out"); out })
            .map {
              _ foreach {
                case r: ReactionMessage =>
                  slackApiClient.addReaction(r.getText, channelId = Some(message.channel), timestamp = Some(message.ts))
                case o: OutboundMessage =>
                  sendMessage(message.channel, o)
              }

            }

        case None =>
          val index = Math.abs(message.text.hashCode + message.user.hashCode) % unrecognisedMsgs.size
          val text = unrecognisedMsgs(index)
          sendMessage(message.channel, SimpleMessage(text, Failure))

      }

  }

  private def sendMessage(channel: String, outboundMessage: OutboundMessage): Unit = {
    slackRtmClient.sendMessage(channel, toSendMessage(channel, outboundMessage))
  }

  private def sendMessage(channel: String, text: String, status: Status) = {
    slackRtmClient.sendMessage(channel, toSendMessage(channel, text, status))
  }

  private def unbundle(outboundMessage: OutboundMessage): Seq[OutboundMessage] = {
    outboundMessage match {
      case MessageBundle(outboundMessages) => outboundMessages
      case singleOutboundMessage => Seq(singleOutboundMessage)
    }
  }

  private def toSendMessage(channel: String, outboundMessage: OutboundMessage): String = {
    toSendMessage(channel, outboundMessage.getText, outboundMessage.status)
  }

  private def toSendMessage(channel: String, text: String, status: Status): String = {
    s"${statusIcon(status)} $text"
  }

}

object LunchbotActor extends Formatting {

  def props(selfId: String,
            messagesService: MessagesService,
            statisticsService: StatisticsService,
            slackRtmClient: SlackRtmClient,
            slackApiClient: BlockingSlackApiClient,
            config: Config): Props = {
    Props(new LunchbotActor(selfId, messagesService, statisticsService, slackRtmClient, slackApiClient, config))
  }

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

  case class ReactionMessage(status: Status) extends OutboundMessage {
    override def getText: String = {
      status match {
        case Success => goodEmoji
        case Failure => badEmoji
      }
    }
  }

}
