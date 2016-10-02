package actors

import actors.EaterActor.{Empty, FoodChosen, FoodData, Joined}
import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import commands.Choose
import org.scalatest.{FlatSpecLike, MustMatchers}

/**
  * Created by mactur on 02/10/2016.
  */
class EaterActorSpec extends TestKit(ActorSystem("EaterActorSpec")) with FlatSpecLike with MustMatchers {

  it should "process food selection" in {

    val eaterActor = TestFSMRef(new EaterActor)

    eaterActor.stateName mustBe Joined
    eaterActor.stateData mustBe Empty

    val eater1 = "some_eater"

    val food1 = "some_food"
    val food2 = "some_other_food"

    eaterActor ! Choose(eater1, food1)

    eaterActor.stateName mustBe FoodChosen
    eaterActor.stateData mustBe FoodData(food1)

    eaterActor ! Choose(eater1, food2)

    eaterActor.stateName mustBe FoodChosen
    eaterActor.stateData mustBe FoodData(food2)

  }

}
