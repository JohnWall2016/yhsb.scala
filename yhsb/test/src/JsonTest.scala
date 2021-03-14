package yhsb

import utest._
import yhsb.base.util.Config
import yhsb.cjb.net.JsonService
import yhsb.cjb.net.protocol.SysLogin
import yhsb.cjb.net.PageRequest
import java.{util => ju}
import yhsb.cjb.net.protocol.CbshQuery

object JsonTest extends TestSuite {
  def tests = Tests {
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
        sortOpts = ju.Map.of("abc001", "asc", "abc002", "dsc")
      )
      println(req.toString())
    }
    test("cbsh") {
      val req = CbshQuery("2020-03-22", "2020-06-23", "1")
      println(req)
    }
  }
}