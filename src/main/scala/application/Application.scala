package application

import akka.actor.ActorSystem
import com.softwaremill.macwire._
import com.typesafe.config.{Config, ConfigFactory}
import net.ceedubs.ficus.Ficus._
import repository.LunchmasterRepository
import service.{LunchbotService, MessagesService}
import slack.rtm.SlackRtmClient
import sql.JdbcConnection

import scala.concurrent.duration.FiniteDuration

class Application()(implicit actorSystem: ActorSystem)
  extends Start
    with Stop {

  val config: Config = ConfigFactory.load()

  val token: String = System.getenv("SLACK_API_KEY")

  val timeout: FiniteDuration = config.as[FiniteDuration]("slack.timeout")

  val client: SlackRtmClient = SlackRtmClient(token, timeout)

  val messagesService: MessagesService = wire[MessagesService]

  val jdbcConnection: JdbcConnection = wire[JdbcConnection]

  val lunchmasterRepository: LunchmasterRepository = wire[LunchmasterRepository]

  val lunchbotService: LunchbotService = wire[LunchbotService]

  override def start(): Unit = {
    lunchbotService.start()
  }

  override def stop(): Unit = {
    lunchbotService.stop()
  }

}

trait Start {

  def start(): Unit

}

trait Stop {

  def stop(): Unit

}
