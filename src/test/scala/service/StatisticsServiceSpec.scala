package service

import akka.actor.ActorSystem
import akka.testkit.TestKit
import application.TestApplicationSpec
import commands.Create
import org.mockito.Mockito
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpecLike, MustMatchers}
import play.api.libs.json.{JsValue, Json}
import slack.api.HistoryChunk

import scala.concurrent.ExecutionContext.Implicits.global

class StatisticsServiceSpec
  extends TestKit(ActorSystem("StatisticsServiceSpec"))
    with FlatSpecLike
    with MustMatchers
    with MockitoSugar
    with TestApplicationSpec {

  it should "calculate lunchmaster statistics" in {

    import Mockito._
    import testApp._
    import messagesService._

    val channel = "test_channel"

    val maxMessages = 42

    val (lunchmaster1, count1) = "lunchmaster_1" -> 1
    val (lunchmaster2, count2) = "lunchmaster_2" -> 2
    val (lunchmaster3, count3) = "lunchmaster_3" -> 3

    val expected = Map(
      lunchmaster1 -> count1,
      lunchmaster2 -> count2,
      lunchmaster3 -> count3
    )

    def foo(lunchmaster: String): JsValue = {
      Json.obj(
        "text" -> messages[Create].created("some_place", lunchmaster)
      )
    }

    val historyMessages: Seq[JsValue] = {
      ((1 to count1) map (_ => foo(lunchmaster1))) ++
        ((1 to count2) map (_ => foo(lunchmaster2))) ++
        ((1 to count3) map (_ => foo(lunchmaster3)))
    }

    when(slackApiClient.getChannelHistory(channel, count = Some(maxMessages)))
      .thenReturn(HistoryChunk(None, historyMessages, has_more = false))

    statisticsService.getLunchmasterStatistics(channel, Some(maxMessages)) mustBe expected

  }

}
