package commands

import slack.models.Message

/**
  * Created by mactur on 02/10/2016.
  */
trait CommandParsing {

  private def nameAndArgs(text: String): Option[(String, Option[String])] = {
    text.split(" ").toSeq match {
      case Nil => None
      case name +: Nil => Some(name -> None)
      case name +: args => Some(name -> Some(args.mkString(" ")))
    }
  }

  def parse(message: Message): Option[Command] = {
    nameAndArgs(message.text.trim) flatMap {
      case ("create", None) => None
      case ("create", Some(place)) => Some(Create(message.user, place))
      case ("join", None) => Some(Join(message.user))
      case ("join", Some(_)) => None
      case ("choose", None) => None
      case ("choose", Some(food)) => Some(Choose(message.user, food))
      case ("cancel", None) => Some(Cancel)
      case ("cancel", Some(_)) => None
      case _ => None
    }
  }

}
