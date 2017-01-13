package modules

import actors.LunchbotActor
import commands.Create

import scala.concurrent.ExecutionContext

trait Statistics {

  _: LunchbotActor =>

  import messagesService._

  def getLunchmasterStatistics(channel: String,
                               maxMessages: Option[Int] = None)
                              (implicit executionContext: ExecutionContext): Map[String, Int] = {

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
                                 (implicit executionContext: ExecutionContext): String = {

    val occurrenceMap = getLunchmasterStatistics(channel, maxMessages)

    val sorted = occurrenceMap.toSeq.sortBy(_._2).reverse

    val occurrenceString = sorted.map {
      case (master, count) => s"• $master [$count]"
    }.mkString("\n")

    s"Recent lunchmasters:\n$occurrenceString"

  }

}
