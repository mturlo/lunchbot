package repository

import application.TestApplicationSpec
import org.scalatest.{FlatSpec, MustMatchers}

class LunchmasterRepositorySpec
  extends FlatSpec
    with MustMatchers
    with TestApplicationSpec {

  it should "persist lunchmasters" in {

    val lunchmasterRepository = testApp.lunchmasterRepository

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
