package util

import model.Statuses.Status
import model._

trait Formatting {

  protected def formatMention(userId: UserId): String = s"<@$userId>"

  protected def formatUrl(input: String): String = input.replaceAll("[<>]", "")

  protected def removeMentions(input: String): String = input.replaceAll("[@<>]", "")

  val goodEmoji = "white_check_mark"
  val badEmoji = "x"

  protected def statusIcon(status: Status): String = {
    status match {
      case Statuses.Success => s":$goodEmoji:"
      case Statuses.Failure => s":$badEmoji:"
    }
  }

}
