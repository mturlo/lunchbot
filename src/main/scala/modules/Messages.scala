package modules

import com.typesafe.config.Config
import commands.{Command, Create}

import scala.reflect.ClassTag
import scala.util.matching.Regex

/**
  * Created by mactur on 27/11/2016.
  */
trait Messages {

  _: Configuration =>

  val messagesConfig: Config = config.getConfig("messages")

  trait CommandMessages[C <: Command] {

    protected def getMessage(key: String)(implicit ev: ClassTag[C]): String = {

      messagesConfig.getString(ev.runtimeClass.getSimpleName.toLowerCase + s".$key")

    }

  }

  implicit class CreateCommandMessages(input: CommandMessages[Create]) extends CommandMessages[Create] {

    def created(lunchmaster: String, place: String): String = getMessage("created").format(lunchmaster, place)

    val created: String = getMessage("created")

    val noLunch: String = getMessage("noLunch")

  }

  implicit class MessageString(input: String) {

    val regex: Regex = (".*" + input.replaceAll("%s", "(.*)")).r

  }

  def messages[C <: Command]: CommandMessages[C] = new CommandMessages[C] {}

}

