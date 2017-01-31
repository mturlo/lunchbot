package application

import akka.actor.ActorSystem
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.scaladsl.{CurrentEventsByPersistenceIdQuery, ReadJournal}
import application.Application._
import com.softwaremill.macwire._
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import service.{LunchbotService, MessagesService, StatisticsService}
import slack.api.BlockingSlackApiClient
import slack.rtm.SlackRtmClient
import util.Logging

import scala.concurrent.duration.FiniteDuration

class Application()(implicit val actorSystem: ActorSystem)
  extends Logging
    with Start
    with Stop {

  val config: Config = ConfigFactory.load()

  lazy val token: String = {
    Option(System.getenv(tokenEnv))
      .getOrElse {
        logger.error(s"Slack token environment variable not set. Please set it via `export $tokenEnv=my_slack_token`")
        System.exit(1)
        ""
      }
  }

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

  val tokenEnv: String = "SLACK_API_KEY"

}

trait Start {

  def start(): Unit

}

trait Stop {

  def stop(): Unit

}
