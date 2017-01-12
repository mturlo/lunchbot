package modules

import com.typesafe.config.Config
import commands._

import scala.reflect.ClassTag
import scala.util.matching.Regex

trait Messages {

  _: Configuration =>

  lazy val messagesConfig: Config = config.getConfig("messages")

  trait CommandMessages[C <: Command] {

    protected def getMessage(key: String)(implicit ev: ClassTag[C]): String = {

      messagesConfig.getString(ev.runtimeClass.getSimpleName.toLowerCase + s".$key")

    }

  }

  implicit class CreateCommandMessages(input: CommandMessages[Create]) extends CommandMessages[Create] {

    def created(place: String, lunchmaster: String): String = created.format(place, lunchmaster)

    val created: String = getMessage("created")

    def alreadyRunning(place: String, lunchmaster: String): String = getMessage("already-running").format(place, lunchmaster)

  }

  implicit class FinishCommandMessages(input: CommandMessages[Finish]) extends CommandMessages[Finish] {

    val finished: String = getMessage("finished")

  }

  implicit class KickCommandMessages(input: CommandMessages[Kick]) extends CommandMessages[Kick] {

    def kicked(eater: String): String = getMessage("kicked").format(eater)

    val notJoined: String = getMessage("not-joined")

  }

  implicit class SummaryCommandMessages(input: CommandMessages[Summary]) extends CommandMessages[Summary] {

    def header(place: String, lunchMaster: String): String = getMessage("header").format(place, lunchMaster)

    def joined(count: Int): String = getMessage("joined").format(count)

    def chosen(count: Int): String = getMessage("chosen").format(count)

    def paid(count: Int): String = getMessage("paid").format(count)

    val nobodyChosen: String = getMessage("nobody-chosen")

    def currentOrder(contents: String): String = getMessage("current-order").format(contents)

  }

  implicit class JoinCommandMessages(input: CommandMessages[Join]) extends CommandMessages[Join] {

    val alreadyJoined: String = getMessage("already-joined")

  }

  implicit class LeaveCommandMessages(input: CommandMessages[Leave]) extends CommandMessages[Leave] {

    def notJoined(place: String): String = getMessage("not-joined").format(place)

  }

  implicit class ChooseCommandMessages(input: CommandMessages[Choose]) extends CommandMessages[Choose] {

    val notJoined: String = getMessage("not-joined")

    val alreadyPaid: String = getMessage("already-paid")

  }

  implicit class PayCommandMessages(input: CommandMessages[Pay]) extends CommandMessages[Pay] {

    val notJoined: String = getMessage("not-joined")

    val notChosen: String = getMessage("not-chosen")

    val alreadyPaid: String = getMessage("already-paid")

  }

  implicit class CloseCommandMessages(input: CommandMessages[Close]) extends CommandMessages[Close] {

    val closed: String = getMessage("closed")

    val someNotChosen: String = getMessage("some-not-chosen")

    val alreadyClosed: String = getMessage("already-closed")

  }

  implicit class OpenCommandMessages(input: CommandMessages[Open]) extends CommandMessages[Open] {

    def opened(place: String): String = getMessage("opened").format(place)

    val alreadyOpen: String = getMessage("already-open")

  }

  implicit class PokeCommandMessages(input: CommandMessages[Poke]) extends CommandMessages[Poke] {

    val notChosen: String = getMessage("not-chosen")

    val notPaid: String = getMessage("not-paid")

  }

  implicit class MessageString(input: String) {

    val regex: Regex = (".*" + input.replaceAll("%s", "(.*)")).r

  }

  def messages[C <: Command]: CommandMessages[C] = new CommandMessages[C] {}

  def message(key: String): String = messagesConfig.getString(key)

}

