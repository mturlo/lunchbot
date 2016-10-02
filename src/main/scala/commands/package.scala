import model.Username

/**
  * Created by mactur on 01/10/2016.
  */
package object commands {

  sealed trait Command

  case class Create(lunchmaster: Username, place: String) extends Command

  case class Join(eater: Username) extends Command

  case class Choose(eater: Username, food: String) extends Command

}
