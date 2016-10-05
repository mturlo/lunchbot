package commands

/**
  * Created by mactur on 05/10/2016.
  */
trait CommandUsage {

  def renderUsage: String = {

    val header = "usage: @lunchbot `[command]` `[args...]`"

    val commands = allCommands.map {

      case oneArg: OneArgCommand[_] =>
        s"• `${oneArg.name}` `<${oneArg.argName}>` - ${oneArg.description}"

      case noArg: NoArgCommand[_] =>
        s"• `${noArg.name}` - ${noArg.description}"

    }

    (header +: commands).mkString("\n")

  }

}
