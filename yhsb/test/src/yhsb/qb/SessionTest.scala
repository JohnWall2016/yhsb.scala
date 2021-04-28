package yhsb.qb

import utest._
import yhsb.qb.net.Session
import yhsb.qb.net.protocol._

object SessionTest extends TestSuite {
  def tests = Tests {
    test("retire") {
      Session.use() { session =>
        val result = session.request(AgencyCodeQuery())

        def getAgencyCode(name: String) = 
          result.resultSet.find(_.name == name).get.code

        session.request(
          JoinedPersonInProvinceQuery("430302195806251012")
        )
        .resultSet.foreach { it =>
          println(it)

          session.request(RetiredPersonQuery(
            it.idCard, getAgencyCode(it.agencyName)
          ))
          .resultSet.foreach(println)
        }
      }
    }
    test("company") {
      Session.use() { session =>
        session.request(CompanyInfoQuery("430302031124"))
          .resultSet.foreach(println)
      }
    }
  }
}