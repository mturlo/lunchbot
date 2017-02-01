package util

import com.typesafe.scalalogging.LazyLogging

trait Logging extends LazyLogging {

  logger.info(s"*** [CREATE] - ${getClass.getSimpleName}")

}
