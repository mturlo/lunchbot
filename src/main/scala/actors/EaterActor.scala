package actors

import actors.EaterActor._
import actors.LunchActor.EaterReport
import actors.LunchbotActor.MentionMessage
import akka.actor.{FSM, Props}
import commands.{Choose, Pay, Poke, Summary}
import model.Statuses._
import model.UserId

/**
  * Created by mactur on 02/10/2016.
  */
class EaterActor(eaterId: UserId) extends FSM[State, Data] {

  startWith(Joined, Empty)

  when(Joined) {

    case Event(Choose(_, food), Empty) =>
      sender ! MentionMessage(s"You've successfully selected: $food as your food", eaterId, Success)
      goto(FoodChosen) using FoodData(food)

    case Event(Pay(payerId), Empty) =>
      sender ! MentionMessage(s"Choose some food first!", payerId, Failure)
      stay

    case Event(Summary(_), Empty) =>
      sender ! EaterReport(eaterId, stateName, stateData)
      stay

    case Event(Poke(_), Empty) =>
      sender ! MentionMessage(s"Hey, everybody's waiting for you! Choose some food already!", eaterId, Failure)
      stay

  }

  when(FoodChosen) {

    case Event(Choose(_, newFood), FoodData(currentFood)) =>
      sender ! MentionMessage(s"You've changed your food selection from $currentFood to $newFood", eaterId, Success)
      stay using FoodData(newFood)

    case Event(Pay(_), _) =>
      sender ! MentionMessage(s"Thanks for paying!", eaterId, Success)
      goto(Paid)

    case Event(Summary(_), _) =>
      sender ! EaterReport(eaterId, stateName, stateData)
      stay

    case Event(Poke.Pay(_), _) =>
      sender ! MentionMessage(s"Where's my money, man?!", eaterId, Failure)
      stay

  }

  when(Paid) {

    case Event(Choose(_, _), _) =>
      sender ! MentionMessage(s"Too late for choosing - you've already paid!", eaterId, Failure)
      stay

    case Event(Pay(payerId), _) =>
      sender ! MentionMessage(s"But you've already paid!", payerId, Failure)
      stay

    case Event(Summary(_), _) =>
      sender ! EaterReport(eaterId, stateName, stateData)
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


  def props(eaterId: UserId): Props = Props(new EaterActor(eaterId))

}
