package config

import akka.actor.ActorSystem
import cats.data.Reader
import com.typesafe.config.Config
import config.DbConfig.{Case, Dialect}
import io.getquill.{H2Dialect, JdbcContext, SnakeCase}
import org.zalando.grafter.GenericReader._
import service.{LunchbotService, MessagesService}
import slack.rtm.SlackRtmClient

case class Application(messagesService: MessagesService,
                       lunchbotService: LunchbotService)

object Application {

  implicit def reader: Reader[ApplicationConfig, Application] = genericReader

}


case class ApplicationConfig(config: Config,
                             actorSystem: ActorSystem,
                             slackRtmClient: SlackRtmClient,
                             jdbcContext: JdbcContext[Dialect, Case])

object DbConfig {

  type Dialect = H2Dialect
  type Case = SnakeCase

}
