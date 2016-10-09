package model

/**
  * Created by mactur on 09/10/2016.
  */
object Statuses extends Enumeration {

  type Status = Value

  val Success = Value(0)
  val Failure = Value(1)

}
