package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.util.Config

object ConfigTest extends TestSuite {
  def tests =
    Tests {
      test("database") {
        val config = Config.load("fullcover2020")
        println(config)
      }
      test("session") {
        val config = Config.load("yhsb.cjb.session")
        println(config)
        println(config.getString("host"))
        println(config.getInt("port"))
        val user002 = config.getConfig("users.002")
        println(user002.getString("id"))
        println(user002.getString("pwd"))
      }
    }
}
