package actors

import actors.EaterActor._
import actors.LunchbotActor.MentionMessage
import akka.actor.{FSM, Props}
import commands.Choose

/**
  * Created by mactur on 02/10/2016.
  */
class EaterActor extends FSM[State, Data] {

  startWith(Joined, Empty)

  when(Joined) {

    case Event(Choose(eaterId, food), Empty) =>
      sender ! MentionMessage(s"You've successfully selected: $food as your food", eaterId)
      goto(FoodChosen) using FoodData(food)

  }

  when(FoodChosen) {

    case Event(Choose(eaterId, newFood), FoodData(currentFood)) =>
      sender ! MentionMessage(s"You've changed your food selection from $currentFood to $newFood", eaterId)
      stay using FoodData(newFood)

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