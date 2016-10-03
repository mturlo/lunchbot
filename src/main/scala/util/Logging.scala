package util

import play.api.Logger

/**
  * Created by mactur on 01/10/2016.
  */
trait Logging {

  val logger: Logger = Logger(getClass)

  logger.info(s"*** [CREATE] - ${getClass.getSimpleName}")

}
