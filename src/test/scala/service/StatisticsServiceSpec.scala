package service

import actors.LunchbotActor.{HereMessage, SimpleMessage}
import actors.{InMemoryCleanup, LunchActor, MessageAssertions}
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import application.TestApplicationSpec
import commands.{Create, Finish}
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

    val expected = Map(
      lunchmaster1 -> count1,
      lunchmaster2 -> count2,
      lunchmaster3 -> count3
    )

    expected foreach {
      case (lunchmaster, count) =>
        (1 to count) foreach { _ =>
          lunchActor ! Create(lunchmaster, "some_place")
          expectSuccess[HereMessage]
          lunchActor ! Finish(lunchmaster)
          expectSuccess[SimpleMessage]
        }
    }

    testApp.statisticsService.getLunchmasterStatistics.futureValue mustBe expected

  }

}
