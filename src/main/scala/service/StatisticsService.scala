package service

import akka.actor.ActorSystem
import commands.Create
import slack.api.BlockingSlackApiClient
import util.Logging

import scala.concurrent.ExecutionContext

class StatisticsService(messagesService: MessagesService,
                        slackApiClient: BlockingSlackApiClient)
  extends Logging {

  import messagesService._

  def getLunchmasterStatistics(channel: String,
                               maxMessages: Option[Int] = None)
                              (implicit executionContext: ExecutionContext,
                               actorSystem: ActorSystem): Map[String, Int] = {

    val createRegex = messages[Create].created.regex

    val historyChunk = slackApiClient.getChannelHistory(channel, count = maxMessages)

    val createLunchMessages = historyChunk.messages.filter { jsMessage =>
      createRegex.findFirstIn((jsMessage \ "text").as[String]).isDefined
    }

    val masters = createLunchMessages.map { jsMessage =>
      val text = (jsMessage \ "text").as[String]
      text match {
        case createRegex(_, master) => master
      }
    }

    masters.groupBy(x => x).mapValues(_.size)

  }

  def renderLunchmasterStatistics(channel: String,
                                  maxMessages: Option[Int] = None)
                                 (implicit executionContext: ExecutionContext,
                                  actorSystem: ActorSystem): String = {

    val occurrenceMap = getLunchmasterStatistics(channel, maxMessages)

    val sorted = occurrenceMap.toSeq.sortBy(_._2).reverse

    val occurrenceString = sorted.map {
      case (master, count) => s"• $master [$count]"
    }.mkString("\n")

    s"Recent lunchmasters:\n$occurrenceString"

  }

}
