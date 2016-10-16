import actors.LunchbotActor
import akka.actor.{ActorRef, ActorSystem}
import slack.rtm.SlackRtmClient

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by mactur on 29/09/2016.
  */
object Main extends App {

  implicit val actorSystem = ActorSystem("slack")

  val token: String = System.getenv("SLACK_API_KEY")

  val timeout: FiniteDuration = 30 seconds

  val client = SlackRtmClient(token, timeout)

  val selfId: String = client.state.self.id

  val lunchbotActor: ActorRef = actorSystem.actorOf(LunchbotActor.props(selfId, client.apiClient), "lunchbot")

  client.addEventListener(lunchbotActor)

  sys.addShutdownHook {
    client.close()
    actorSystem.terminate()
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)

}
