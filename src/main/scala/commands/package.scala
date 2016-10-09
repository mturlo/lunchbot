import model.UserId

/**
  * Created by mactur on 01/10/2016.
  */
package object commands {

  sealed trait Command

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

  case class Create(lunchmaster: UserId, place: String) extends Command

  case class Cancel(canceller: UserId) extends Command

  case class Summary(caller: UserId) extends Command

  case class Poke(poker: UserId) extends Command

  case class Kick(kicker: UserId, kicked: UserId) extends Command

  case class Join(eater: UserId) extends Command

  case class Leave(eater: UserId) extends Command

  case class Choose(eater: UserId, food: String) extends Command

  case class Pay(payer: UserId) extends Command

  case class Help(caller: UserId) extends Command

  object Create extends OneArgCommand {
    override def name: String = "create"

    override def description: String = s"creates a new lunch at `<$argName>`"

    override def argName: String = "name or URL of the place"
  }

  object Cancel extends NoArgCommand {
    override def name: String = "cancel"

    override def description: String = "cancels current lunch"
  }

  object Summary extends NoArgCommand {
    override def name: String = "summary"

    override def description: String = "returns lunch summary"
  }

  object Poke extends NoArgCommand {
    override def name: String = "poke"

    override def description: String = "pokes all eaters that are lazy with their order"
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

  val allCommands = Seq(
    Create,
    Cancel,
    Summary,
    Poke,
    Kick,
    Join,
    Leave,
    Choose,
    Pay,
    Help
  )

}
