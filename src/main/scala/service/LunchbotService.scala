package service

import actors.LunchbotActor
import akka.actor.{ActorRef, ActorSystem}
import cats.Eval
import cats.data.Reader
import com.typesafe.config.Config
import config.ApplicationConfig
import org.zalando.grafter.{Start, StartResult, Stop, StopResult}
import slack.rtm.SlackRtmClient

case class LunchbotService(actorSystem: ActorSystem,
                           slackRtmClient: SlackRtmClient,
                           config: Config)
  extends Start
    with Stop {

  val lunchbotActor: ActorRef = {
    val props = LunchbotActor.props(
      slackRtmClient.state.self.id,
      slackRtmClient.apiClient,
      config)
    actorSystem.actorOf(props, "lunchbot")
  }

  override def start: Eval[StartResult] = {
    StartResult.eval(getClass.getName) {
      slackRtmClient.addEventListener(lunchbotActor)
    }
  }

  override def stop: Eval[StopResult] = {
    StopResult.eval(getClass.getName) {
      slackRtmClient.close()
    }
  }

}

object LunchbotService {

  implicit def reader: Reader[ApplicationConfig, LunchbotService] = {
    Reader { applicationConfig =>
      LunchbotService(
        applicationConfig.actorSystem,
        applicationConfig.slackRtmClient,
        applicationConfig.config
      )
    }
  }

}
