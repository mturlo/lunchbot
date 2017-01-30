package model

object Statuses extends Enumeration {

  type Status = Value

  val Success = Value(0)
  val Failure = Value(1)

}
