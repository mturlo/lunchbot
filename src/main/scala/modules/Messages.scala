package modules

import commands.{Command, Create}

import scala.reflect.ClassTag

/**
  * Created by mactur on 27/11/2016.
  */
trait Messages {

  _: Configuration =>

  trait CommandMessages[C <: Command] {

    protected def getMessage(key: String)(implicit ev: ClassTag[C]): String = {

      config.getString(ev.runtimeClass.getSimpleName.toLowerCase + s".$key")

    }

  }

  implicit class CreateCommandMessages(input: CommandMessages[Create]) extends CommandMessages[Create] {

    val created: String = getMessage("created")

    val noLunch: String = getMessage("noLunch")

  }

  def messages[C <: Command]: CommandMessages[C] = new CommandMessages[C] {}

}

