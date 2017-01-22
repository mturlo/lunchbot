package service

import actors.LunchbotActor
import akka.actor.{ActorRef, ActorSystem}
import application.{Start, Stop}
import com.typesafe.config.Config
import slack.rtm.SlackRtmClient
import util.Logging

class LunchbotService(messagesService: MessagesService,
                      statisticsService: StatisticsService,
                      slackRtmClient: SlackRtmClient,
                      actorSystem: ActorSystem,
                      config: Config)
  extends Logging
    with Start
    with Stop {

  val lunchbotActor: ActorRef = {
    val props = LunchbotActor.props(
      slackRtmClient.state.self.id,
      messagesService,
      statisticsService,
      slackRtmClient.apiClient,
      config)
    actorSystem.actorOf(props, "lunchbot")
  }

  override def start(): Unit = {
    slackRtmClient.addEventListener(lunchbotActor)
  }

  override def stop(): Unit = {
    slackRtmClient.close()
  }

}
