package actors

import actors.LunchActor.{Empty, Idle, InProgress, LunchData}
import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import commands._
import org.scalatest.{FlatSpecLike, MustMatchers}

/**
  * Created by mactur on 02/10/2016.
  */
class LunchActorSpec extends TestKit(ActorSystem("LunchActorSpec")) with FlatSpecLike with MustMatchers {

  it should "process lunch creation and cancellation" in {

    val lunchActor = TestFSMRef(new LunchActor)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    val lunchmaster1 = "some_lunchmaster"
    val place1 = "some_place"

    // creating a new lunch

    lunchActor ! Create(lunchmaster1, place1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster1, place1, Map.empty)

    // second create should have no effect

    lunchActor ! Create(lunchmaster1, place1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster1, place1, Map.empty)

    // cancelling the lunch

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    // second cancel should have no effect

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    val lunchmaster2 = "some_other_lunchmaster"
    val place2 = "some_other_place"

    // creating a new lunch by another lunchmaster

    lunchActor ! Create(lunchmaster2, place2)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Map.empty)

    // only the current lunchmaster can cancel the lunch

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Map.empty)

  }

  it should "process eater joins" in {

    val lunchActor = TestFSMRef(new LunchActor)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    val lunchmaster1 = "some_lunchmaster"
    val place1 = "some_place"

    // creating a new lunch

    lunchActor ! Create(lunchmaster1, place1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster1, place1, Map.empty)

    val eater1 = "some_eater"
    val eater2 = "some_other_eater"

    // first eater joins

    lunchActor ! Join(eater1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1

    // second join should have no effect

    lunchActor ! Join(eater1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1

    // second eater joins

    lunchActor ! Join(eater2)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 2
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater2

  }

}
