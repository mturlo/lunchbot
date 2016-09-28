import actors.LunchbotActor
import akka.actor.{ActorRef, ActorSystem, Props}
import com.cyberdolphins.slime.SlackBotActor.{Close, Connect}

/**
  * Created by mactur on 29/09/2016.
  */
object Main extends App {

  val actorSystem = ActorSystem()

  val lunchbotActor: ActorRef = actorSystem.actorOf(Props[LunchbotActor], "lunchbot")

  lunchbotActor ! Connect(System.getenv("SLACK_API_KEY"))

  sys.addShutdownHook {
    lunchbotActor ! Close
    actorSystem.shutdown()
    actorSystem.awaitTermination()
  }

  actorSystem.awaitTermination()

}
