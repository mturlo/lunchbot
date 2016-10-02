package commands

import model.UserId
import org.scalatest.{FlatSpec, MustMatchers}
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
    parse(getMessage("create")) mustBe None
    parse(getMessage("create ")) mustBe None
    parse(getMessage(" create")) mustBe None
    parse(getMessage(" create ")) mustBe None
    parse(getMessage("create a")) mustBe Some(Create(testUser, "a"))
    parse(getMessage("create a ")) mustBe Some(Create(testUser, "a"))
    parse(getMessage(" create a")) mustBe Some(Create(testUser, "a"))
    parse(getMessage("create a b c")) mustBe Some(Create(testUser, "a b c"))
    parse(getMessage("createa b c")) mustBe None
    parse(getMessage("create a b c ")) mustBe Some(Create(testUser, "a b c"))
  }

  it should "parse join command" in new CommandParsing {
    parse(getMessage("join")) mustBe Some(Join(testUser))
    parse(getMessage(" join")) mustBe Some(Join(testUser))
    parse(getMessage(" join ")) mustBe Some(Join(testUser))
    parse(getMessage("join a")) mustBe None
    parse(getMessage("join a ")) mustBe None
    parse(getMessage("joina")) mustBe None
    parse(getMessage(" join a ")) mustBe None
  }

  it should "parse cancel command" in new CommandParsing {
    parse(getMessage("cancel")) mustBe Some(Cancel)
    parse(getMessage(" cancel")) mustBe Some(Cancel)
    parse(getMessage(" cancel ")) mustBe Some(Cancel)
    parse(getMessage("cancel a")) mustBe None
    parse(getMessage("cancel a ")) mustBe None
    parse(getMessage("cancela")) mustBe None
    parse(getMessage(" cancel a ")) mustBe None
  }

  it should "parse choose command" in new CommandParsing {
    parse(getMessage("choose")) mustBe None
    parse(getMessage("choosea")) mustBe None
    parse(getMessage(" choosea")) mustBe None
    parse(getMessage(" choose")) mustBe None
    parse(getMessage(" choose ")) mustBe None
    parse(getMessage("choose a")) mustBe Some(Choose(testUser, "a"))
    parse(getMessage("choose a ")) mustBe Some(Choose(testUser, "a"))
    parse(getMessage("choose a b c")) mustBe Some(Choose(testUser, "a b c"))
    parse(getMessage("choose a b c ")) mustBe Some(Choose(testUser, "a b c"))
  }

}