package application

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import repository.LunchmasterRepository
import service.{LunchbotService, MessagesService, StatisticsService}
import slack.rtm.SlackRtmClient
import sql.JdbcConnection

import scala.concurrent.duration.FiniteDuration

class Application()(implicit actorSystem: ActorSystem)
  extends Start
    with Stop {

  val config: Config = ConfigFactory.load()

  val token: String = System.getenv("SLACK_API_KEY")

  val timeout: FiniteDuration = config.as[FiniteDuration]("slack.timeout")

  lazy val slackRtmClient: SlackRtmClient = SlackRtmClient(token, timeout)

  lazy val messagesService: MessagesService = wire[MessagesService]

  lazy val jdbcConnection: JdbcConnection = wire[JdbcConnection]

  lazy val lunchmasterRepository: LunchmasterRepository = wire[LunchmasterRepository]

  lazy val statisticsService: StatisticsService = wire[StatisticsService]

  lazy val lunchbotService: LunchbotService = wire[LunchbotService]

  override def start(): Unit = {
    lunchbotService.start()
  }

  override def stop(): Unit = {
    lunchbotService.stop()
    jdbcConnection.jdbcContext.close()
  }

}

trait Start {

  def start(): Unit

}

trait Stop {

  def stop(): Unit

}
