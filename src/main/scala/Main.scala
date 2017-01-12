import actors.LunchbotActor
import akka.actor.{ActorRef, ActorSystem}
import com.typesafe.config.{Config, ConfigFactory}
import slack.rtm.SlackRtmClient

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  implicit val actorSystem = ActorSystem("slack")

  val token: String = System.getenv("SLACK_API_KEY")

  val timeout: FiniteDuration = 30 seconds

  val client = SlackRtmClient(token, timeout)

  val selfId: String = client.state.self.id

  val config: Config = ConfigFactory.load()

  val lunchbotActor: ActorRef = actorSystem.actorOf(LunchbotActor.props(selfId, client.apiClient, config), "lunchbot")

  client.addEventListener(lunchbotActor)

  sys.addShutdownHook {
    client.close()
    actorSystem.terminate()
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)

}
