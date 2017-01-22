package sql

import io.getquill.{H2Dialect, JdbcContext, SnakeCase}

class JdbcConnection {

  val jdbcContext = new JdbcContext[H2Dialect, SnakeCase]("storage")

}
