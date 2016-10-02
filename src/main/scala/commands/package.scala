import model.UserId

/**
  * Created by mactur on 01/10/2016.
  */
package object commands {

  sealed trait Command

  case class Create(lunchmaster: UserId, place: String) extends Command

  case class Join(eater: UserId) extends Command

  case class Choose(eater: UserId, food: String) extends Command

  case object Cancel extends Command

}
