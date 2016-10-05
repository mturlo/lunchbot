package commands

import model.UserId
import util.Formatting

/**
  * Created by mactur on 05/10/2016.
  */
trait CommandUsage {

  self: Formatting =>

  def renderUsage(selfId: UserId): String = {

    val header = s"usage: ${formatMention(selfId)} `[command]` `[args...]`"

    val commands = allCommands.map {

      case oneArg: OneArgCommand =>
        s"• `${oneArg.name}` `<${oneArg.argName}>` - ${oneArg.description}"

      case noArg: NoArgCommand =>
        s"• `${noArg.name}` - ${noArg.description}"

    }

    (header +: commands).mkString("\n")

  }

}
