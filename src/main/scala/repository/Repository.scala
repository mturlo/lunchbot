package repository

import sql.JdbcConnection

trait Repository {

  val jdbcConnection: JdbcConnection

  val ctx = jdbcConnection.jdbcContext

}
