package yhsb.qb


import utest._
import yhsb.qb.net.Session
import yhsb.qb.net.protocol._

object SessionTest extends TestSuite {
  def tests = Tests {
    test("agencyCode") {
      Session.use() { session =>
        val result = session.request(AgencyCodeQuery())
        println(result)
      }
    }
  }
}