package actors

import actors.LunchActor.{Empty, Idle, InProgress, LunchData}
import akka.actor.ActorSystem
import akka.testkit.{TestFSMRef, TestKit}
import commands._
import org.scalatest.{FlatSpec, FlatSpecLike, MustMatchers}

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

    lunchActor ! Create(lunchmaster1, place1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster1, place1, Nil)

    lunchActor ! Create(lunchmaster1, place1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster1, place1, Nil)

    lunchActor ! Cancel

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    lunchActor ! Cancel

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    val lunchmaster2 = "some_other_lunchmaster"
    val place2 = "some_other_place"

    lunchActor ! Create(lunchmaster2, place2)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Nil)

  }

}
