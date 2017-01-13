package repository

import config.DbConfig.{Case, Dialect}
import io.getquill.{H2Dialect, JdbcContext, SnakeCase}
import util.Logging
import model._

case class LunchmasterRepository(ctx: JdbcContext[Dialect, Case])
  extends Logging {

  import ctx._

  case class Lunchmaster(id: UserId)

  object Lunchmaster {

    val create: String =
      """create table if not exists
        |lunchmaster (
        | id varchar primary key
        |);""".stripMargin

  }

  ctx.executeAction(Lunchmaster.create)

  def add(lunchmaster: Lunchmaster): Long = {
    ctx.run(query[Lunchmaster].insert(lift(lunchmaster)))
  }

  def addAll(lunchmasters: Seq[Lunchmaster]): Long = {
    lunchmasters map add sum
  }

  def delete(id: UserId): Long = {
    ctx.run(query[Lunchmaster].filter(_.id == lift(id)).delete)
  }

  def deleteAll(ids: Seq[UserId]): Long = {
    ids map delete sum
  }

  private[repository] def truncate = {
    ctx.run(query[Lunchmaster].delete)
  }

}
