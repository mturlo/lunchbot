package commands

import org.scalatest.FlatSpec
import util.Formatting

/**
  * Created by mactur on 05/10/2016.
  */
class CommandUsageSpec extends FlatSpec {

  it should "render command usage" in new CommandUsage with Formatting {
    val selfId = "lunchbot"
    println(renderUsage(selfId))
  }

}
