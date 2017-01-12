package commands

import model.UserId

sealed trait Command {

  val caller: UserId

}

sealed trait CommandDescription {

  def name: String

  def description: String

}

sealed trait NoArgCommand extends CommandDescription {

  def apply(caller: UserId): Command

}

sealed trait OneArgCommand extends CommandDescription {

  def apply(caller: UserId, arg: String): Command

  def argName: String

}

case class Create(caller: UserId, place: String) extends Command

case class Close(caller: UserId) extends Command

case class Open(caller: UserId) extends Command

case class Finish(caller: UserId) extends Command

case class Summary(caller: UserId) extends Command

case class Poke(caller: UserId) extends Command

case class Kick(caller: UserId, kicked: UserId) extends Command

case class Join(caller: UserId) extends Command

case class Leave(caller: UserId) extends Command

case class Choose(caller: UserId, food: String) extends Command

case class Pay(caller: UserId) extends Command

case class Help(caller: UserId) extends Command

case class Stats(caller: UserId) extends Command

case class Unhandled(caller: UserId) extends Command

object Create extends OneArgCommand {
  override def name: String = "create"

  override def description: String = s"creates a new lunch at `<$argName>`"

  override def argName: String = "name or URL of the place"
}

object Close extends NoArgCommand {
  override def name: String = "close"

  override def description: String = "closes current lunch for order changes"
}

object Open extends NoArgCommand {
  override def name: String = "open"

  override def description: String = "opens current lunch for further order changes"
}

object Finish extends NoArgCommand {
  override def name: String = "finish"

  override def description: String = "finishes current lunch"
}

object Summary extends NoArgCommand {
  override def name: String = "summary"

  override def description: String = "returns lunch summary"
}

object Poke extends NoArgCommand {
  override def name: String = "poke"

  override def description: String = "pokes all eaters that are lazy with their order"

  case class Pay(caller: UserId) extends Command

}

object Kick extends OneArgCommand {
  override def name: String = "kick"

  override def description: String = s"kicks `<$argName>` from the current lunch"

  override def argName: String = "eater to kick"
}

object Join extends NoArgCommand {
  override def name: String = "join"

  override def description: String = "joins the current lunch"
}

object Leave extends NoArgCommand {
  override def name: String = "leave"

  override def description: String = "leaves the current lunch"
}

object Choose extends OneArgCommand {
  override def name: String = "choose"

  override def description: String = s"chooses food with `<$argName>`"

  override def argName: String = "food name"
}

object Pay extends NoArgCommand {
  override def name: String = "pay"

  override def description: String = "notifies about payment being made"
}

object Help extends NoArgCommand {
  override def name: String = "help"

  override def description: String = "prints this usage text"
}

object Stats extends NoArgCommand {
  override def name: String = "stats"

  override def description: String = "prints historic orders statistics"
}

object Commands {

  val allCommands = Seq(
    Create,
    Close,
    Open,
    Finish,
    Summary,
    Poke,
    Kick,
    Join,
    Leave,
    Choose,
    Pay,
    Help,
    Stats
  )

}
