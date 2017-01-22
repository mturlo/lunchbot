package sql

import application.Stop
import io.getquill.{H2Dialect, JdbcContext, SnakeCase}
import util.Logging

class JdbcConnection
  extends Logging
    with Stop {

  val jdbcContext = new JdbcContext[H2Dialect, SnakeCase]("storage")

  override def stop(): Unit = {
    jdbcContext.close()
  }

}
