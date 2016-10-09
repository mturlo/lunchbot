package util

import model.Statuses.Status
import model._

/**
  * Created by mactur on 02/10/2016.
  */
trait Formatting {

  protected def formatMention(userId: UserId): String = s"<@$userId>"

  protected def formatUrl(input: String): String = input.replaceAll("[<>]", "")

  protected def removeMentions(input: String): String = input.replaceAll("[@<>]", "")

  protected def statusIcon(status: Status): String = {
    status match {
      case Statuses.Success => ":white_check_mark:"
      case Statuses.Failure => ":x:"
    }
  }

}
