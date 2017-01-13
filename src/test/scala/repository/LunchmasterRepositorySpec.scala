package repository

import config.DbConfig.{Case, Dialect}
import io.getquill.JdbcContext
import org.scalatest.{BeforeAndAfterEach, FlatSpec, MustMatchers}

class LunchmasterRepositorySpec
  extends FlatSpec
    with MustMatchers
    with BeforeAndAfterEach {

  lazy val jdbcContext = new JdbcContext[Dialect, Case]("storage") // todo create test app config

  val lunchmasterRepository = LunchmasterRepository(jdbcContext)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    lunchmasterRepository.truncate
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    lunchmasterRepository.ctx.close()
  }

  it should "persist lunchmasters" in {

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
