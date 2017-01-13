package service

import actors.LunchbotActor
import akka.actor.{ActorRef, ActorSystem}
import cats.Eval
import cats.data.Reader
import com.typesafe.config.Config
import config.ApplicationConfig
import org.zalando.grafter.{Start, StartResult, Stop, StopResult}
import org.zalando.grafter.GenericReader._
import slack.rtm.SlackRtmClient
import util.Logging

case class LunchbotService(messagesService: MessagesService,
                           lunchbotServiceConfig: LunchbotServiceConfig)
  extends Logging
    with Start
    with Stop {

  val lunchbotActor: ActorRef = {
    val props = LunchbotActor.props(
      lunchbotServiceConfig.slackRtmClient.state.self.id,
      messagesService,
      lunchbotServiceConfig.slackRtmClient.apiClient,
      lunchbotServiceConfig.config)
    lunchbotServiceConfig.actorSystem.actorOf(props, "lunchbot")
  }

  override def start: Eval[StartResult] = {
    StartResult.eval(getClass.getName) {
      lunchbotServiceConfig.slackRtmClient.addEventListener(lunchbotActor)
    }
  }

  override def stop: Eval[StopResult] = {
    StopResult.eval(getClass.getName) {
      lunchbotServiceConfig.slackRtmClient.close()
    }
  }

}

case class LunchbotServiceConfig(actorSystem: ActorSystem,
                                 slackRtmClient: SlackRtmClient,
                                 config: Config)

object LunchbotServiceConfig {

  implicit def reader: Reader[ApplicationConfig, LunchbotServiceConfig] = {
    Reader { applicationConfig =>
      LunchbotServiceConfig(
        applicationConfig.actorSystem,
        applicationConfig.slackRtmClient,
        applicationConfig.config
      )
    }
  }

}

object LunchbotService {

  implicit def reader: Reader[ApplicationConfig, LunchbotService] = genericReader

}
