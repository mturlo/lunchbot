package service

import actors.LunchbotActor.{HereMessage, SimpleMessage}
import actors.{InMemoryCleanup, LunchActor, MessageAssertions}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import application.TestApplicationSpec
import commands.{Create, Finish}
import model.LunchmasterStatistics
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FlatSpecLike, MustMatchers}

import scala.concurrent.ExecutionContext

class StatisticsServiceSpec
  extends TestKit(ActorSystem("StatisticsServiceSpec"))
    with ImplicitSender
    with FlatSpecLike
    with MustMatchers
    with ScalaFutures
    with Eventually
    with MessageAssertions
    with InMemoryCleanup
    with TestApplicationSpec {

  it should "calculate lunchmaster statistics" in {

    implicit val executionContext: ExecutionContext = system.dispatcher

    val lunchActor = system.actorOf(LunchActor.props(testApp.messagesService))

    val (lunchmaster1, count1) = "lunchmaster_1" -> 1
    val (lunchmaster2, count2) = "lunchmaster_2" -> 2
    val (lunchmaster3, count3) = "lunchmaster_3" -> 3

    val expected = Seq(
      LunchmasterStatistics(lunchmaster1,  count1, "Lunch Initiate"),
      LunchmasterStatistics(lunchmaster2,  count2, "Junior Ordering Assistant"),
      LunchmasterStatistics(lunchmaster3,  count3, "Food Delivery Executive")
    )

    expected foreach { stats =>
        (1 to stats.lunchCount) foreach { _ =>
          lunchActor ! Create(stats.userId, "some_place")
          expectSuccess[HereMessage]
          lunchActor ! Finish(stats.userId)
          expectSuccess[SimpleMessage]
        }
    }

    testApp.statisticsService.getLunchmasterStatistics.futureValue mustBe expected

  }

}
