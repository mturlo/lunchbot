package actors

import actors.EaterActor._
import actors.LunchbotActor.MentionMessage
import akka.actor.{FSM, Props}
import commands.{Choose, Pay}

/**
  * Created by mactur on 02/10/2016.
  */
class EaterActor extends FSM[State, Data] {

  startWith(Joined, Empty)

  when(Joined) {

    case Event(Choose(eaterId, food), Empty) =>
      sender ! MentionMessage(s"You've successfully selected: $food as your food", eaterId)
      goto(FoodChosen) using FoodData(food)

    case Event(Pay(payerId), Empty) =>
      sender ! MentionMessage(s"Choose some food first!", payerId)
      stay

  }

  when(FoodChosen) {

    case Event(Choose(eaterId, newFood), FoodData(currentFood)) =>
      sender ! MentionMessage(s"You've changed your food selection from $currentFood to $newFood", eaterId)
      stay using FoodData(newFood)

    case Event(Pay(payerId), _) =>
      sender ! MentionMessage(s"Thanks for paying!", payerId)
      goto(Paid)

  }

  when(Paid) {

    case Event(Choose(eaterId, _), _) =>
      sender ! MentionMessage(s"Too late for choosing - you've already paid!", eaterId)
      stay

    case Event(Pay(payerId), _) =>
      sender ! MentionMessage(s"But you've already paid!", payerId)
      stay

  }

  initialize

}

object EaterActor {

  sealed trait State

  case object Joined extends State

  case object FoodChosen extends State

  case object Paid extends State

  sealed trait Data

  case object Empty extends Data

  case class FoodData(food: String) extends Data


  def props: Props = Props[EaterActor]

}