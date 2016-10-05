import model.UserId

/**
  * Created by mactur on 01/10/2016.
  */
package object commands {

  sealed trait Command

  sealed trait NoArgCommand[T] extends Command {

    def apply(caller: UserId): T

    def name: String

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

  object Create extends OneArgCommand[Create] {
    override def argName: String = "name or URL of the place"

    override def name: String = "create"
  }

  object Cancel extends NoArgCommand[Cancel] {
    override def name: String = "cancel"
  }

  object Summary extends NoArgCommand[Summary] {
    override def name: String = "summary"
  }

  object Poke extends NoArgCommand[Poke] {
    override def name: String = "poke"
  }

  object Join extends NoArgCommand[Join] {
    override def name: String = "join"
  }

  object Choose extends OneArgCommand[Choose] {
    override def argName: String = "food name"

    override def name: String = "choose"
  }

  object Pay extends NoArgCommand[Pay] {
    override def name: String = "pay"
  }

  val allCommands = Set(
    Create,
    Cancel,
    Summary,
    Poke,
    Join,
    Choose,
    Pay
  )

}
