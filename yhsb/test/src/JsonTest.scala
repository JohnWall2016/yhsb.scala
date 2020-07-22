package yhsb

import utest._
import yhsb.util.Config
import yhsb.cjb.net.{SysLogin, JsonService}

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
  }
}