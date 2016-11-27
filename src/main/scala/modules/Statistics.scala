package modules

import scala.concurrent.ExecutionContext
import scala.util.matching.Regex

/**
  * Created by mactur on 26/11/2016.
  */
trait Statistics {

  _: SlackApi =>

  def getLunchmasterStatistics(channel: String,
                               createRegex: Regex,
                               maxMessages: Option[Int] = None)
                              (implicit executionContext: ExecutionContext): Map[String, Int] = {

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
                                  createRegex: Regex,
                                  maxMessages: Option[Int] = None)
                                 (implicit executionContext: ExecutionContext): String = {

    val occurrenceMap = getLunchmasterStatistics(channel, createRegex, maxMessages)

    val sorted = occurrenceMap.toSeq.sortBy(_._2).reverse

    val occurrenceString = sorted.map {
      case (master, count) => s"• $master [$count]"
    }.mkString("\n")

    s"Recent lunchmasters:\n$occurrenceString"

  }

}
