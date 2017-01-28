package util

import org.slf4j.{Logger, LoggerFactory}

trait Logging {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  logger.info(s"*** [CREATE] - ${getClass.getSimpleName}")

}
