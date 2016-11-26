package modules

import slack.api.BlockingSlackApiClient

/**
  * Created by mactur on 26/11/2016.
  */
trait SlackApi {

  val slackApiClient: BlockingSlackApiClient

}
