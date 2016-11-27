package actors

import actors.EaterActor._
import actors.LunchActor.EaterReport
import actors.LunchbotActor.{MentionMessage, ReactionMessage}
import akka.actor.{FSM, Props}
import com.typesafe.config.Config
import commands.{Choose, Pay, Poke, Summary}
import model.Statuses._
import model.UserId
import modules.{Configuration, Messages}

/**
  * Created by mactur on 02/10/2016.
  */
class EaterActor(eaterId: UserId,
                 override val config: Config)
  extends FSM[State, Data]
    with Configuration
    with Messages {

  startWith(Joined, Empty)

  when(Joined) {

    case Event(Choose(_, food), Empty) =>
      sender ! ReactionMessage(Success)
      goto(FoodChosen) using FoodData(food)

    case Event(Pay(payerId), Empty) =>
      sender ! MentionMessage(messages[Pay].notChosen, payerId, Failure)
      stay

    case Event(Summary(_), Empty) =>
      sender ! EaterReport(eaterId, stateName, stateData)
      stay

    case Event(Poke(_), Empty) =>
      sender ! MentionMessage(messages[Poke].notChosen, eaterId, Failure)
      stay

  }

  when(FoodChosen) {

    case Event(Choose(_, newFood), _) =>
      sender ! ReactionMessage(Success)
      stay using FoodData(newFood)

    case Event(Pay(_), _) =>
      sender ! ReactionMessage(Success)
      goto(Paid)

    case Event(Summary(_), _) =>
      sender ! EaterReport(eaterId, stateName, stateData)
      stay

    case Event(Poke.Pay(_), _) =>
      sender ! MentionMessage(messages[Poke].notPaid, eaterId, Failure)
      stay

  }

  when(Paid) {

    case Event(Choose(_, _), _) =>
      sender ! MentionMessage(messages[Choose].alreadyPaid, eaterId, Failure)
      stay

    case Event(Pay(payerId), _) =>
      sender ! MentionMessage(messages[Pay].alreadyPaid, payerId, Failure)
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


  def props(eaterId: UserId, config: Config): Props = Props(new EaterActor(eaterId, config))

}
