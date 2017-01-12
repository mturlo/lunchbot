package modules

import slack.api.BlockingSlackApiClient

trait SlackApi {

  val slackApiClient: BlockingSlackApiClient

}
