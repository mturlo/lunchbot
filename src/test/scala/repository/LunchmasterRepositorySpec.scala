package repository

import application.TestApplication
import org.scalatest.{BeforeAndAfterEach, FlatSpec, MustMatchers}

class LunchmasterRepositorySpec
  extends FlatSpec
    with MustMatchers
    with BeforeAndAfterEach {

  val testApplication = new TestApplication

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    testApplication.lunchmasterRepository.truncate
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    testApplication.lunchmasterRepository.ctx.close()
  }

  it should "persist lunchmasters" in {

    val lunchmasterRepository = testApplication.lunchmasterRepository

    import lunchmasterRepository._

    val l1 = Lunchmaster("1")
    val l2 = Lunchmaster("2")
    val l3 = Lunchmaster("3")

    lunchmasterRepository.add(l1) mustBe 1

    lunchmasterRepository.addAll(Seq(l2, l3)) mustBe 2

    lunchmasterRepository.delete(l1.id) mustBe 1

    lunchmasterRepository.deleteAll(Seq(l2, l3).map(_.id)) mustBe 2

  }

}
