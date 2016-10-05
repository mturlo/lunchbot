package commands

import model.UserId
import org.scalatest.{Assertion, FlatSpec, MustMatchers}
import slack.models.Message

/**
  * Created by mactur on 02/10/2016.
  */
class CommandParsingSpec extends FlatSpec with MustMatchers {

  val testUser: UserId = "test_user"

  private def getMessage(text: String): Message = {
    Message("", "", testUser, text, None)
  }

  it should "parse create command" in new CommandParsing {
    assertArgCommand("create", Create.apply, parse)
  }

  it should "parse cancel command" in new CommandParsing {
    assertNoArgCommand("cancel", Cancel.apply, parse)
  }

  it should "parse join command" in new CommandParsing {
    assertNoArgCommand("join", Join.apply, parse)
  }

  it should "parse choose command" in new CommandParsing {
    assertArgCommand("choose", Choose.apply, parse)
  }

  it should "parse pay command" in new CommandParsing {
    assertNoArgCommand("pay", Pay.apply, parse)
  }

  it should "parse summary command" in new CommandParsing {
    assertNoArgCommand("summary", Summary.apply, parse)
  }

  it should "parse poke command" in new CommandParsing {
    assertNoArgCommand("poke", Poke.apply, parse)
  }

  it should "parse help command" in new CommandParsing {
    assertNoArgCommand("help", Help.apply, parse)
  }

  type ParseFunction = Message => Option[Command]

  def assertArgCommand(commandName: String,
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