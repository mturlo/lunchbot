package service

import actors.LunchActor.LunchCreated
import akka.NotUsed
import akka.actor.ActorSystem
import akka.persistence.query.EventEnvelope
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import application.Application.EventReadJournal
import com.softwaremill.tagging.@@
import com.typesafe.config.Config
import model.{LunchmasterStatistics, UserId}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ValueReader
import util.{Formatting, Logging}
import scala.collection.JavaConverters._
import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}

class StatisticsService(actorSystem: ActorSystem,
                        eventReadJournal: EventReadJournal,
                        statisticsConfig: Config @@ StatisticsService)
  extends Logging
    with Formatting {

  implicit val mat: ActorMaterializer = ActorMaterializer()(actorSystem)

  implicit val titlesMapReader: ValueReader[Seq[(Int, String)]] = (config: Config, path: String) => {
    val entries = config.getConfig(path).entrySet()
    val tuples = entries.asScala.map { entry =>
      entry.getKey.toInt -> entry.getValue.unwrapped().asInstanceOf[String]
    }
    tuples.toSeq
  }

  private val titlesByLevel: Seq[(Int, String)] = statisticsConfig.as[Seq[(Int, String)]]("titles").sortBy(_._1)

  def getLunchmasterStatistics(implicit executionContext: ExecutionContext,
                               actorSystem: ActorSystem): Future[Seq[LunchmasterStatistics]] = async {

    val src: Source[EventEnvelope, NotUsed] = eventReadJournal.currentEventsByPersistenceId("LunchActor", 0L, Long.MaxValue)

    val events: Source[Any, NotUsed] = src.map(_.event)

    val lunchCountByUser = await {
      events.runFold(Map.empty[UserId, Int]) {
        case (acc, LunchCreated(lunchmaster, _)) =>
          acc + (lunchmaster -> (acc.getOrElse(lunchmaster, 0) + 1))
        case (acc, _)                            =>
          acc
      }
    }

    lunchCountByUser map {
      case (userId, count) => LunchmasterStatistics(userId, count, getTitle(count))
    } toSeq

  }

  private def getTitle(lunchCount: Int): String = {
    titlesByLevel
      .takeWhile(_._1 <= lunchCount)
      .last
      ._2
  }

}
