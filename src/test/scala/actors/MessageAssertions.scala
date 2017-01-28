package actors

import actors.LunchbotActor.OutboundMessage
import akka.testkit.TestKitBase
import model.Statuses
import model.Statuses.Status
import org.scalatest.{Assertion, MustMatchers}
import scala.concurrent.duration._

import scala.reflect.ClassTag

trait MessageAssertions {

  self: TestKitBase
    with MustMatchers =>

  def expectSuccess[T <: OutboundMessage: ClassTag]: Assertion = expectStatus[T](Statuses.Success)

  def expectFailure[T <: OutboundMessage: ClassTag]: Assertion = expectStatus[T](Statuses.Failure)

  private def expectStatus[T <: OutboundMessage: ClassTag](expected: Status): Assertion = {
    expectMsgPF(1 second) {
      case out: T => out.status must be(expected)
    }
  }

}
