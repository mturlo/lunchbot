package service

import actors.LunchActor.LunchCreated
import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.EventEnvelope
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import application.Application.EventReadJournal
import model.UserId
import util.{Formatting, Logging}

import scala.concurrent.{ExecutionContext, Future}

class StatisticsService(actorSystem: ActorSystem,
                        eventReadJournal: EventReadJournal)
  extends Logging
    with Formatting {

  implicit val mat: ActorMaterializer = ActorMaterializer()(actorSystem)

  def getLunchmasterStatistics(implicit executionContext: ExecutionContext,
                               actorSystem: ActorSystem): Future[Map[UserId, Int]] = {

    val src: Source[EventEnvelope, NotUsed] = eventReadJournal.currentEventsByPersistenceId("LunchActor", 0L, Long.MaxValue)

    val events: Source[Any, NotUsed] = src.map(_.event)

    events.runFold(Map.empty[UserId, Int]) {
      case (acc, LunchCreated(lunchmaster, _)) =>
        acc + (lunchmaster -> (acc.getOrElse(lunchmaster, 0) + 1))
      case (acc, _) =>
        acc
    }

  }

  def renderLunchmasterStatistics(implicit executionContext: ExecutionContext,
                                  actorSystem: ActorSystem): Future[String] = {

    getLunchmasterStatistics map { occurrenceMap =>

      val sorted = occurrenceMap.toSeq.sortBy(_._2).reverse

      val occurrenceString = sorted.map {
        case (master, count) => s"• ${formatMention(master)} [$count]"
      }.mkString("\n")

      s"Recent lunchmasters:\n$occurrenceString"

    }

  }

}
