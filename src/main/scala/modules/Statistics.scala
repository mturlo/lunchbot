package modules

import scala.concurrent.ExecutionContext

/**
  * Created by mactur on 26/11/2016.
  */
trait Statistics {

  _: SlackApi =>

  def getLunchmasterStatistics(channel: String,
                               createPattern: String,
                               maxMessages: Option[Int] = None)
                              (implicit executionContext: ExecutionContext): Map[String, Int] = {

    val regex = ".* Created new lunch instance at: (.*) with (.*) as Lunchmaster"
    val pattern = regex.r //todo

    val historyChunk = slackApiClient.getChannelHistory(channel, count = maxMessages)

    val createLunchMessages = historyChunk.messages.filter { jsMessage =>
      (jsMessage \ "text").as[String].matches(regex)
    }

    val masters = createLunchMessages.map { jsMessage =>
      val text = (jsMessage \ "text").as[String]
      text match {
        case pattern(_, master) => master
      }
    }

    masters.groupBy(x => x).mapValues(_.size)

  }

  def renderLunchmasterStatistics(channel: String,
                                  createPattern: String,
                                  maxMessages: Option[Int] = None)
                                 (implicit executionContext: ExecutionContext): String = {

    val occurrenceMap = getLunchmasterStatistics(channel, createPattern, maxMessages)

    val sorted = occurrenceMap.toSeq.sortBy(_._2).reverse

    val occurrenceString = sorted.map {
      case (master, count) => s"• $master [$count]"
    }.mkString("\n")

    s"Recent lunchmasters:\n$occurrenceString"

  }

}
