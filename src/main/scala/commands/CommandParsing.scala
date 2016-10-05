package commands

import slack.models.Message

/**
  * Created by mactur on 02/10/2016.
  */
trait CommandParsing {

  type CommandPartial = PartialFunction[(String, Option[String]), Option[Command]]

  private def nameAndArgs(text: String): Option[(String, Option[String])] = {
    text.split("\\s").toSeq match {
      case Nil => None
      case name +: Nil => Some(name -> None)
      case name +: args => Some(name -> Some(args.mkString(" ")))
    }
  }

  def parse(message: Message): Option[Command] = {

    val nonAppliedPartials: Set[String => CommandPartial] = {
      allCommands.map {
        case oneArg: OneArgCommand[_] => oneArgCommand(oneArg.name, oneArg.apply) _
        case noArg: NoArgCommand[_] => noArgCommand(noArg.name, noArg.apply) _
      }
    }

    val appliedPartials: Set[CommandPartial] = nonAppliedPartials.map(_.apply(message.user))

    val reducedPartials: CommandPartial = appliedPartials.reduce(_ orElse _)

    val unhandledPartial: CommandPartial = {
      case _ => None
    }

    nameAndArgs(message.text.trim) flatMap {
      reducedPartials orElse unhandledPartial
    }

  }

  private def noArgCommand(name: String, command: String => Command)(caller: String): CommandPartial = {
    case (`name`, None) => Some(command(caller))
    case (`name`, Some(_)) => None
  }

  private def oneArgCommand(name: String, command: (String, String) => Command)(caller: String): CommandPartial = {
    case (`name`, None) => None
    case (`name`, Some(arg)) => Some(command(caller, arg.trim))
  }

}
