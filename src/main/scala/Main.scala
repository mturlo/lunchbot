import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import config.DbConfig.{Case, Dialect}
import config.{Application, ApplicationConfig}
import io.getquill.JdbcContext
import org.zalando.grafter.GenericReader
import slack.rtm.SlackRtmClient

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

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

  val application: Application = {
    GenericReader[ApplicationConfig, Application]
      .run(applicationConfig)
  }

  sys.addShutdownHook {
    client.close()
    actorSystem.terminate()
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)

}
