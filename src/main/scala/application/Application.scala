package application

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, ReadJournal}
import application.Application.EventReadJournal
import com.softwaremill.macwire._
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import service.{LunchbotService, MessagesService, StatisticsService}
import slack.api.BlockingSlackApiClient
import slack.rtm.SlackRtmClient

import scala.concurrent.duration.FiniteDuration

class Application()(implicit val actorSystem: ActorSystem)
  extends Start
    with Stop {

  val config: Config = ConfigFactory.load()

  val token: String = System.getenv("SLACK_API_KEY")

  val timeout: FiniteDuration = config.as[FiniteDuration]("slack.timeout")

  val eventReadJournal: EventReadJournal = {
    PersistenceQuery(actorSystem)
      .readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)
  }

  lazy val slackRtmClient: SlackRtmClient = SlackRtmClient(token, timeout)

  lazy val slackApiClient: BlockingSlackApiClient = BlockingSlackApiClient(token, timeout)

  lazy val messagesService: MessagesService = wire[MessagesService]

  lazy val statisticsService: StatisticsService = wire[StatisticsService]

  lazy val lunchbotService: LunchbotService = wire[LunchbotService]

  override def start(): Unit = {
    lunchbotService.start()
  }

  override def stop(): Unit = {
    lunchbotService.stop()
  }

}

object Application {

  type EventReadJournal = ReadJournal with CurrentEventsByPersistenceIdQuery

}

trait Start {

  def start(): Unit

}

trait Stop {

  def stop(): Unit

}
