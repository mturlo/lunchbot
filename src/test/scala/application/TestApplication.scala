package application

import akka.actor.ActorSystem
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import slack.api.BlockingSlackApiClient
import slack.rtm.SlackRtmClient

class TestApplication
  extends Application()(ActorSystem("test"))
    with MockitoSugar {

  import Mockito._

  lazy val mockSlackApi: BlockingSlackApiClient = mock[BlockingSlackApiClient]

  override lazy val slackRtmClient: SlackRtmClient = mock[SlackRtmClient]

  when(slackRtmClient.apiClient).thenReturn(mockSlackApi)

}
