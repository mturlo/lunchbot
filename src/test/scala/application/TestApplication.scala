package application

import akka.actor.ActorSystem
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import slack.api.{BlockingSlackApiClient, RtmStartState}
import slack.models.User
import slack.rtm.{RtmState, SlackRtmClient}

class TestApplication
  extends Application()(ActorSystem("test"))
    with MockitoSugar {

  import Mockito._

  override lazy val slackApiClient: BlockingSlackApiClient = mock[BlockingSlackApiClient]

  override lazy val slackRtmClient: SlackRtmClient = mock[SlackRtmClient]

  when(slackRtmClient.state).thenReturn {
    RtmState {
      RtmStartState(
      null,
      User("test_user_id", "test_user_name", None, None, None, None, None, None, None, None, None, None, None, None, None, None),
      null,
      Nil,
      Nil,
      Nil,
      Nil,
      Nil
      )
    }
  }

}

trait TestApplicationSpec extends BeforeAndAfterEach {

  _: Suite =>

  protected val testApp = new TestApplication

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    testApp.start()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    testApp.stop()
  }

}
