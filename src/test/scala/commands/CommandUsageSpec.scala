package commands

import org.scalatest.FlatSpec

/**
  * Created by mactur on 05/10/2016.
  */
class CommandUsageSpec extends FlatSpec {

  it should "render command usage" in new CommandUsage {
    println(renderUsage)
  }

}
