package actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import model.UserId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, MustMatchers}
import slack.models.Message
import slack.rtm.SlackRtmConnectionActor.SendMessage
import util.Formatting

/**
  * Created by mactur on 05/10/2016.
  */
class LunchbotActorSpec
  extends TestKit(ActorSystem("LunchbotActorSpec"))
    with FlatSpecLike
    with MustMatchers
    with ImplicitSender
    with ScalaFutures
    with Formatting {

  val testUser = "test_user"
  val selfId = "some_self_id"

  private def getMessage(text: String, userId: UserId = testUser): Message = {
    Message("", "", userId, text, None)
  }

  it should "only react when mentioned" in {

    val lunchbotActor = TestActorRef(LunchbotActor.props(selfId))

    val messageWithMention = getMessage(s"<@$selfId> hey there!")

    lunchbotActor ! messageWithMention

    expectMsgClass(classOf[SendMessage])

    val messageWithoutMention = getMessage("hey there!")

    lunchbotActor ! messageWithoutMention

    expectNoMsg()

  }

  it should "not react to own mentions" in {

    val lunchbotActor = TestActorRef(LunchbotActor.props(selfId))

    val messageWithMention = getMessage(s"<@$selfId> hey there!", selfId)

    lunchbotActor ! messageWithMention

    expectNoMsg()

  }

}
