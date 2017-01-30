package application

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import application.Application.EventReadJournal
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import slack.api.{BlockingSlackApiClient, RtmStartState}
import slack.models.User
import slack.rtm.{RtmState, SlackRtmClient}

class TestApplication(implicit actorSystem: ActorSystem)
  extends Application()(actorSystem)
    with MockitoSugar {

  import Mockito._

  override val eventReadJournal: EventReadJournal = {
    PersistenceQuery(implicitly[ActorSystem])
      .readJournalFor("inmemory-read-journal")
      .asInstanceOf[EventReadJournal]
  }

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

  implicit val system: ActorSystem

  protected val testApp = new TestApplication()(system)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    testApp.start()
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    testApp.stop()
  }

}
