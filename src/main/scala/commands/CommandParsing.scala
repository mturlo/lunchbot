package commands

import slack.models.Message

/**
  * Created by mactur on 02/10/2016.
  */
trait CommandParsing {

  private def nameAndArgs(text: String): Option[(String, Option[String])] = {
    text.split("\\s").toSeq match {
      case Nil => None
      case name +: Nil => Some(name -> None)
      case name +: args => Some(name -> Some(args.mkString(" ")))
    }
  }

  def parse(message: Message): Option[Command] = {
    val commands = Set(
      oneArgCommand("create", Create.apply) _,
      noArgCommand("cancel", Cancel.apply) _,
      noArgCommand("summary", Summary.apply) _,
      noArgCommand("poke", Poke.apply) _,
      noArgCommand("join", Join.apply) _,
      oneArgCommand("choose", Choose.apply) _,
      noArgCommand("pay", Pay.apply) _
    )
    nameAndArgs(message.text.trim) flatMap {
      commands
        .map(_ (message.user))
        .reduce(_ orElse _)
        .orElse { case _ => None }
    }
  }

  private def noArgCommand(name: String, command: String => Command)(caller: String): PartialFunction[(String, Option[String]), Option[Command]] = {
    case (`name`, None) => Some(command(caller))
    case (`name`, Some(_)) => None
  }

  private def oneArgCommand(name: String, command: (String, String) => Command)(caller: String): PartialFunction[(String, Option[String]), Option[Command]] = {
    case (`name`, None) => None
    case (`name`, Some(arg)) => Some(command(caller, arg.trim))
  }

}
