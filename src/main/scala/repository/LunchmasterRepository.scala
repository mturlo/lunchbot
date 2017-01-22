package repository

import application.Start
import model._
import sql.JdbcConnection
import util.Logging

case class LunchmasterRepository(jdbcConnection: JdbcConnection)
  extends Logging
    with Start
    with Repository {

  import ctx._

  case class Lunchmaster(id: UserId)

  object Lunchmaster {

    val create: String =
      """create table if not exists
        |lunchmaster (
        | id varchar primary key
        |);""".stripMargin

  }

  override def start(): Unit = {
    ctx.executeAction(Lunchmaster.create)
  }

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
