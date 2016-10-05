import model.UserId

/**
  * Created by mactur on 01/10/2016.
  */
package object commands {

  sealed trait Command

  sealed trait NoArgCommand[T] extends Command {

    def apply(caller: UserId): T

    def name: String

    def description: String

  }

  sealed trait OneArgCommand[T] extends NoArgCommand[T] {

    override def apply(caller: UserId): T = apply(caller, "")

    def apply(caller: UserId, arg: String): T

    def argName: String

  }

  case class Create(lunchmaster: UserId, place: String) extends Command

  case class Cancel(canceller: UserId) extends Command

  case class Summary(caller: UserId) extends Command

  case class Poke(poker: UserId) extends Command

  case class Join(eater: UserId) extends Command

  case class Choose(eater: UserId, food: String) extends Command

  case class Pay(payer: UserId) extends Command

  case class Help(caller: UserId) extends Command

  object Create extends OneArgCommand[Create] {
    override def name: String = "create"

    override def description: String = s"creates a new lunch at `<$argName>`"

    override def argName: String = "name or URL of the place"
  }

  object Cancel extends NoArgCommand[Cancel] {
    override def name: String = "cancel"

    override def description: String = "cancels current lunch"
  }

  object Summary extends NoArgCommand[Summary] {
    override def name: String = "summary"

    override def description: String = "returns lunch summary"
  }

  object Poke extends NoArgCommand[Poke] {
    override def name: String = "poke"

    override def description: String = "pokes all eaters that are lazy with their order"
  }

  object Join extends NoArgCommand[Join] {
    override def name: String = "join"

    override def description: String = "joins the current lunch"
  }

  object Choose extends OneArgCommand[Choose] {
    override def name: String = "choose"

    override def description: String = s"chooses food with `<$argName>`"

    override def argName: String = "food name"
  }

  object Pay extends NoArgCommand[Pay] {
    override def name: String = "pay"

    override def description: String = "notifies about payment being made"
  }

  object Help extends NoArgCommand[Help] {
    override def name: String = "help"

    override def description: String = "prints this usage text"
  }

  val allCommands = Seq(
    Create,
    Cancel,
    Summary,
    Poke,
    Join,
    Choose,
    Pay,
    Help
  )

}
