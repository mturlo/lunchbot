import akka.actor.ActorSystem
import cats._
import com.typesafe.config.{Config, ConfigFactory}
import config.DbConfig.{Case, Dialect}
import config.{Application, ApplicationConfig}
import io.getquill.JdbcContext
import org.zalando.grafter.{GenericReader, Rewriter, StartResult, StopResult}
import service.{LunchbotService, MessagesService}
import slack.rtm.SlackRtmClient
import util.Logging

import scala.concurrent.Await
import scala.concurrent.duration._

object Main
  extends App
    with Logging {

  implicit val actorSystem = ActorSystem("slack")

  val token: String = System.getenv("SLACK_API_KEY")

  val timeout: FiniteDuration = 30 seconds

  val client = SlackRtmClient(token, timeout)

  val config: Config = ConfigFactory.load()

  lazy val jdbcContext = new JdbcContext[Dialect, Case]("storage")

  val applicationConfig: ApplicationConfig = {
    ApplicationConfig(
      config,
      actorSystem,
      client,
      jdbcContext
    )
  }

  import Rewriter._

  val application: Application = {
    GenericReader[ApplicationConfig, Application].run(applicationConfig)
      .singleton[MessagesService]
      .singleton[LunchbotService]
  }

  val started: Eval[List[StartResult]] = Rewriter.start(application)

  val startResult = started.value

  logger.info(s"Lunchbot started with result: $startResult")

  sys.addShutdownHook {
    val stopped: Eval[List[StopResult]] = Rewriter.stop(application)
    val stopResult = stopped.value
    logger.info(s"Lunchbot stopped with result: $stopResult")
    actorSystem.terminate()
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)

}
