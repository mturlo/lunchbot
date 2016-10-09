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
    with MustMatchers
    with MessageAssertions {

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

    expectSuccess[HereMessage]

    // second create should have no effect

    lunchActor ! Create(lunchmaster1, place1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster1, place1, Map.empty)

    expectFailure[SimpleMessage]

    // cancelling the lunch

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    expectSuccess[SimpleMessage]

    // second cancel should have no effect

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    expectFailure[SimpleMessage]

    val lunchmaster2 = "some_other_lunchmaster"
    val place2 = "some_other_place"

    // creating a new lunch by another lunchmaster

    lunchActor ! Create(lunchmaster2, place2)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Map.empty)

    expectSuccess[HereMessage]

    // only the current lunchmaster can cancel the lunch

    lunchActor ! Cancel(lunchmaster1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Map.empty)

    expectFailure[SimpleMessage]

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

    expectSuccess[HereMessage]

    val eater1 = "some_eater"
    val eater2 = "some_other_eater"

    // first eater joins

    lunchActor ! Join(eater1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1

    expectSuccess[MentionMessage]

    // second join should have no effect

    lunchActor ! Join(eater1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1

    expectFailure[MentionMessage]

    // second eater joins

    lunchActor ! Join(eater2)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe a[LunchData]
    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 2
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater1
    lunchActor.stateData.asInstanceOf[LunchData].eaters must contain key eater2

    expectSuccess[MentionMessage]

  }

  it should "process eater leaves" in {

    val lunchActor = TestFSMRef(new LunchActor)

    val lunchmaster = "some_lunchmaster"
    val place = "some_place"

    // lunchmaster creates lunch

    lunchActor ! Create(lunchmaster, place)

    expectSuccess[HereMessage]

    val eater1 = "some_eater"
    val eater2 = "some_other_eater"
    val eater3 = "yet_another_eater"

    // eaters join the lunch

    lunchActor ! Join(eater1)
    lunchActor ! Join(eater2)
    lunchActor ! Join(eater3)

    expectSuccess[MentionMessage]
    expectSuccess[MentionMessage]
    expectSuccess[MentionMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 3

    // one eater leaves

    lunchActor ! Leave(eater1)

    expectSuccess[MentionMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 2

    // further leaves for the eater have no effect

    lunchActor ! Leave(eater1)

    expectFailure[MentionMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 2

  }

  it should "poke eaters" in {

    val lunchActor = TestFSMRef(new LunchActor)

    val lunchmaster = "some_lunchmaster"
    val place = "some_place"

    // lunchmaster creates lunch

    lunchActor ! Create(lunchmaster, place)

    expectSuccess[HereMessage]

    val eater1 = "some_eater"
    val eater2 = "some_other_eater"
    val eater3 = "yet_another_eater"

    // eaters join the lunch

    lunchActor ! Join(eater1)
    lunchActor ! Join(eater2)
    lunchActor ! Join(eater3)

    expectSuccess[MentionMessage]
    expectSuccess[MentionMessage]
    expectSuccess[MentionMessage]

    // lunchmaster pokes them

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 3
    }

    // one eater chooses food

    lunchActor ! Choose(eater1, "some food")

    expectSuccess[MentionMessage]

    // lunchmaster pokes the other two

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 2
    }

  }

  it should "kick eaters" in {

    val lunchActor = TestFSMRef(new LunchActor)

    val lunchmaster = "some_lunchmaster"
    val place = "some_place"

    // lunchmaster creates lunch

    lunchActor ! Create(lunchmaster, place)

    expectSuccess[HereMessage]

    val eater1 = "some_eater"
    val eater2 = "some_other_eater"

    // eaters join the lunch

    lunchActor ! Join(eater1)
    lunchActor ! Join(eater2)

    expectSuccess[MentionMessage]
    expectSuccess[MentionMessage]

    // lunchmaster kicks one eater

    lunchActor ! Kick(lunchmaster, eater1)

    expectSuccess[SimpleMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1

    // kicks by other users have no effect

    lunchActor ! Kick(eater1, eater2)

    expectFailure[SimpleMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1

    // kicking the same user again has no effect

    lunchActor ! Kick(lunchmaster, eater1)

    expectFailure[SimpleMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 1

  }

}
