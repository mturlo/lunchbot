package actors

import actors.LunchActor._
import actors.LunchbotActor._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import application.{Application, TestApplication}
import commands._
import org.scalatest.concurrent.Eventually
import org.scalatest.{FlatSpecLike, MustMatchers}

class LunchActorSpec
  extends TestKit(ActorSystem("LunchActorSpec"))
    with ImplicitSender
    with FlatSpecLike
    with MustMatchers
    with Eventually
    with MessageAssertions {

  it should "process lunch creation and finishing" in new TestApplication {

    val lunchActor = TestFSMRef(new LunchActor(messagesService))

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

    // finishing the lunch

    lunchActor ! Finish(lunchmaster1)

    lunchActor.stateName mustBe Idle
    lunchActor.stateData mustBe Empty

    expectSuccess[SimpleMessage]

    // second finish should have no effect

    lunchActor ! Finish(lunchmaster1)

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

    // only the current lunchmaster can finish the lunch

    lunchActor ! Finish(lunchmaster1)

    lunchActor.stateName mustBe InProgress
    lunchActor.stateData mustBe LunchData(lunchmaster2, place2, Map.empty)

    expectFailure[SimpleMessage]

  }

  it should "process eater joins" in new TestApplication {

    val lunchActor = TestFSMRef(new LunchActor(messagesService))

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

    expectSuccess[ReactionMessage]

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

    expectSuccess[ReactionMessage]

  }

  it should "process eater leaves" in new TestApplication {

    val lunchActor = TestFSMRef(new LunchActor(messagesService))

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

    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 3

    // one eater leaves

    lunchActor ! Leave(eater1)

    expectSuccess[ReactionMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 2

    // further leaves for the eater have no effect

    lunchActor ! Leave(eater1)

    expectFailure[MentionMessage]

    lunchActor.stateData.asInstanceOf[LunchData].eaters must have size 2

  }

  it should "poke eaters" in new TestApplication {

    val lunchActor = TestFSMRef(new LunchActor(messagesService))

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

    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]

    // lunchmaster pokes them

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 3
    }

    // one eater chooses food

    lunchActor ! Choose(eater1, "some food")

    expectSuccess[ReactionMessage]

    // lunchmaster pokes the other two

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 2
    }

    // other pokers choose food

    lunchActor ! Choose(eater2, "some food")
    lunchActor ! Choose(eater3, "some food")

    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]

    // lunchmaster closes the order

    lunchActor ! Close(lunchmaster)

    expectSuccess[HereMessage]

    eventually(lunchActor.stateName mustBe Closed)

    // lunchmaster pokes eaters for payment

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 3
    }

    // one eater pays

    lunchActor ! Pay(eater1)

    expectSuccess[ReactionMessage]

    // lunchmaster pokes the other two

    lunchActor ! Poke(lunchmaster)

    expectMsgPF() {
      case MessageBundle(messages) => messages must have size 2
    }

  }

  it should "kick eaters" in new TestApplication {

    val lunchActor = TestFSMRef(new LunchActor(messagesService))

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

    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]

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

  it should "close lunch order" in new TestApplication {

    val lunchActor = TestFSMRef(new LunchActor(messagesService))

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

    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]

    // closing while not lunchmaster fails

    lunchActor ! Close(eater1)

    expectFailure[SimpleMessage]

    lunchActor.stateName mustBe InProgress

    // trying to close lunch with unfinished orders fails

    lunchActor ! Close(lunchmaster)

    expectFailure[SimpleMessage]

    eventually(lunchActor.stateName mustBe InProgress)

    // eaters choose their food

    lunchActor ! Choose(eater1, "food")
    lunchActor ! Choose(eater2, "food")

    expectSuccess[ReactionMessage]
    expectSuccess[ReactionMessage]

    // now it's fine to close the lunch

    lunchActor ! Close(lunchmaster)

    expectSuccess[HereMessage]

    eventually(lunchActor.stateName mustBe Closed)

    // closing a closed lunch does nothing

    lunchActor ! Close(lunchmaster)

    expectFailure[SimpleMessage]

    lunchActor.stateName mustBe Closed

  }

  it should "open a closed lunch" in new TestApplication {

    val lunchActor = TestFSMRef(new LunchActor(messagesService))

    val lunchmaster = "some_lunchmaster"
    val place = "some_place"

    // lunchmaster creates lunch

    lunchActor ! Create(lunchmaster, place)

    expectSuccess[HereMessage]

    // lunchmaster closes lunch

    lunchActor ! Close(lunchmaster)

    expectSuccess[HereMessage]

    lunchActor.stateName mustBe Closed

    // eater cannot reopen lunch
    val eater1 = "some_eater"

    lunchActor ! Open(eater1)

    expectFailure[SimpleMessage]

    lunchActor.stateName mustBe Closed

    // lunchmaster reopens lunch

    lunchActor ! Open(lunchmaster)

    expectSuccess[SimpleMessage]

    lunchActor.stateName mustBe InProgress

    // second opening does nothing

    lunchActor ! Open(lunchmaster)

    expectFailure[SimpleMessage]

    lunchActor.stateName mustBe InProgress

  }

}
