package commands

import model.UserId
import org.scalatest.{Assertion, FlatSpec, MustMatchers}
import slack.models.Message

class CommandParsingSpec extends FlatSpec with MustMatchers {

  val testUser: UserId = "test_user"

  private def getMessage(text: String): Message = {
    Message("", "", testUser, text, None, None)
  }

  it should "parse all commands" in new CommandParsing {
    Commands.allCommands foreach {
      case oneArg: OneArgCommand => assertOneArgCommand(oneArg.name, oneArg.apply, parse)
      case noArg: NoArgCommand => assertNoArgCommand(noArg.name, noArg.apply, parse)
    }
  }

  type ParseFunction = Message => Option[Command]

  def assertOneArgCommand(commandName: String,
                          expected: (UserId, String) => Command,
                          parse: ParseFunction): Assertion = {

    parse(getMessage(s"$commandName")) mustBe None
    parse(getMessage(s"${commandName}a")) mustBe None
    parse(getMessage(s" ${commandName}a")) mustBe None
    parse(getMessage(s" $commandName")) mustBe None
    parse(getMessage(s" $commandName ")) mustBe None
    parse(getMessage(s"$commandName a")) mustBe Some(expected(testUser, "a"))
    parse(getMessage(s"$commandName\na")) mustBe Some(expected(testUser, "a"))
    parse(getMessage(s"$commandName a ")) mustBe Some(expected(testUser, "a"))
    parse(getMessage(s"$commandName a b c")) mustBe Some(expected(testUser, "a b c"))
    parse(getMessage(s"$commandName a b c ")) mustBe Some(expected(testUser, "a b c"))

  }

  def assertNoArgCommand(commandName: String,
                         expected: UserId => Command,
                         parse: ParseFunction): Assertion = {

    parse(getMessage(s"$commandName")) mustBe Some(expected(testUser))
    parse(getMessage(s" $commandName")) mustBe Some(expected(testUser))
    parse(getMessage(s" $commandName ")) mustBe Some(expected(testUser))
    parse(getMessage(s"$commandName a")) mustBe None
    parse(getMessage(s"$commandName a ")) mustBe None
    parse(getMessage(s"${commandName}a")) mustBe None
    parse(getMessage(s" $commandName a ")) mustBe None

  }

}
