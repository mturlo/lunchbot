package actors

import actors.LunchActor.{Empty, Idle, InProgress, LunchData}
import actors.LunchbotActor._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import commands._
import org.scalatest.{FlatSpecLike, MustMatchers}

/**
  * Created by mactur on 02/10/2016.
  */
class LunchActorSpec
  extends TestKit(ActorSystem("LunchActorSpec"))
    with ImplicitSender
    with FlatSpecLike
    with MustMatchers {

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

    expectMsgType[HereMessage]

    // second create should have no effect

    lunchActor ! Create(lunchmaster1, place1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster1, place1, Map.empty)

    expectMsgType[SimpleMessage]

    // cancelling the lunch

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    expectMsgType[SimpleMessage]

    // second cancel should have no effect

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    expectMsgType[SimpleMessage]

    val lunchmaster2 = "some_other_lunchmaster"
    val place2 = "some_other_place"

    // creating a new lunch by another lunchmaster

    lunchActor ! Create(lunchmaster2, place2)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Map.empty)

    expectMsgType[HereMessage]

    // only the current lunchmaster can cancel the lunch

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Map.empty)

    expectMsgType[SimpleMessage]

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

    expectMsgType[HereMessage]

    val eater1 = "some_eater"
    val eater2 = "some_other_eater"

    // first eater joins

    lunchActor ! Join(eater1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1

    expectMsgType[MentionMessage]

    // second join should have no effect

    lunchActor ! Join(eater1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1

    expectMsgType[MentionMessage]

    // second eater joins

    lunchActor ! Join(eater2)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 2
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater2

    expectMsgType[MentionMessage]

  }

  it should "poke eaters" in {

    val lunchActor = TestFSMRef(new LunchActor)

    val lunchmaster = "some_lunchmaster"
    val place = "some_place"

    // lunchmaster creates lunch

    lunchActor ! Create(lunchmaster, place)

    expectMsgType[HereMessage]

    val eater1 = "some_eater"
    val eater2 = "some_other_eater"
    val eater3 = "yet_another_eater"

    // eaters join the lunch

    lunchActor ! Join(eater1)
    lunchActor ! Join(eater2)
    lunchActor ! Join(eater3)

    expectMsgType[MentionMessage]
    expectMsgType[MentionMessage]
    expectMsgType[MentionMessage]

    // lunchmaster pokes them

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 3
    }

    // one eater chooses food

    lunchActor ! Choose(eater1, "some food")

    expectMsgType[MentionMessage]

    // lunchmaster pokes the other two

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 2
    }

  }

}
