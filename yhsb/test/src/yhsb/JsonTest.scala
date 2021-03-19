package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.util.Config
import yhsb.cjb.net.protocol.{
  JoinAuditQuery,
  JsonService,
  PageRequest,
  SysLogin
}
import java.{util => ju}

object JsonTest extends TestSuite {
  def tests =
    Tests {
      test("syslogin") {
        val user = Config.load("cjb.session.users.002")
        val login = SysLogin(user.getString("id"), user.getString("pwd"))
        println(login.toString())

        val service = new JsonService(
          login,
          user.getString("id"),
          user.getString("pwd")
        )
        println(service.toString())
      }
      test("pagereq") {
        val req = new PageRequest(
          "reqid",
          pageSize = 16,
          sortOptions = Map("abc001" -> "asc", "abc002" -> "dsc")
        )
        println(req.toString())
      }
      test("cbsh") {
        val req = JoinAuditQuery("2020-03-22", "2020-06-23", "1")
        println(req)
      }
    }
}
