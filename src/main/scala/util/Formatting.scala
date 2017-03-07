package util

import model.Statuses.Status
import model._

trait Formatting {

  protected def formatMention(userId: UserId): String = s"<@$userId>"

  protected def formatUrl(input: String): String = input.replaceAll("[<>]", "")

  protected def removeMentions(input: String): String = input.replaceAll("[@<>]", "")

  protected def formatStatistics(lunchmasterStatistics: Seq[LunchmasterStatistics]): String = {
    val sorted = lunchmasterStatistics.sortBy(_.lunchCount).reverse
    val occurrenceString = sorted.map { stats =>
      s"• ${formatMention(stats.userId)} [${stats.lunchCount}] (${stats.title})"
    }.mkString("\n")
    s"Lunchmaster statistics:\n$occurrenceString"
  }

  val goodEmoji = "white_check_mark"
  val badEmoji = "x"

  protected def statusIcon(status: Status): String = {
    status match {
      case Statuses.Success => s":$goodEmoji:"
      case Statuses.Failure => s":$badEmoji:"
    }
  }

}
