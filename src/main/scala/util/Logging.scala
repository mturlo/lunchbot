package util

import play.api.Logger

trait Logging {

  val logger: Logger = Logger(getClass)

  logger.info(s"*** [CREATE] - ${getClass.getSimpleName}")

}
