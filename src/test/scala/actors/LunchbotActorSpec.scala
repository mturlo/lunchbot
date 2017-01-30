package actors

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import application.TestApplicationSpec
import model.UserId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, MustMatchers}
import slack.models.Message
import util.Formatting

class LunchbotActorSpec
  extends TestKit(ActorSystem("LunchbotActorSpec"))
    with FlatSpecLike
    with MustMatchers
    with ImplicitSender
    with ScalaFutures
    with MockitoSugar
    with Formatting
    with TestApplicationSpec {

  val testUser = "test_user"
  val selfId = "some_self_id"

  private def getMessage(text: String, userId: UserId = testUser): Message = {
    Message("", "", userId, text, None)
  }

  it should "only react when mentioned" in {

    import testApp.{actorSystem => _, _}

    val lunchbotActor = TestActorRef(LunchbotActor.props(selfId, messagesService, statisticsService, slackRtmClient, slackApiClient, config))

    val messageWithMention: Message = getMessage(s"<@$selfId> hey there!")

    lunchbotActor ! messageWithMention

    // todo expect sendMessage call on mocked api

    val messageWithoutMention: Message = getMessage("hey there!")

    lunchbotActor ! messageWithoutMention

    // todo expect no sendMessage call on mocked api

  }

  it should "not react to own mentions" in {

    import testApp.{actorSystem => _, _}

    val lunchbotActor = TestActorRef(LunchbotActor.props(selfId, messagesService, statisticsService, slackRtmClient, slackApiClient, config))

    val messageWithMention: Message = getMessage(s"<@$selfId> hey there!", selfId)

    lunchbotActor ! messageWithMention

    // todo expect no sendMessage call on mocked api

  }

}
