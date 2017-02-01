import akka.actor.ActorSystem
import application.Application
import util.Logging

import scala.concurrent.Await
import scala.concurrent.duration._

object Main
  extends App
    with Logging {

  implicit val actorSystem = ActorSystem("Lunchbot")

  val application: Application = new Application

  application.start()

  sys.addShutdownHook {
    application.stop()
  }

  Await.result(actorSystem.whenTerminated, Duration.Inf)

}
