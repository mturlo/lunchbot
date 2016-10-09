package util

import model._

/**
  * Created by mactur on 02/10/2016.
  */
trait Formatting {

  protected def formatMention(userId: UserId): String = s"<@$userId>"

  protected def formatUrl(input: String): String = input.replaceAll("[<>]", "")

  protected def removeMentions(input: String): String = input.replaceAll("[@<>]", "")

}
