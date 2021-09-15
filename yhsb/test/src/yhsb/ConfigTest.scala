package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.util.Config
import yhsb.base.collection.BiMap

object ConfigTest extends TestSuite {
  def tests =
    Tests {
      test("database") {
        val config = Config.load("fullcover2020")
        println(config)
      }
      test("session") {
        val config = Config.load("cjb.session")
        println(config)
        println(config.getString("host"))
        println(config.getInt("port"))
        val user002 = config.getConfig("users.002")
        println(user002.getString("id"))
        println(user002.getString("pwd"))
        val users = config.getConfig("users")

        var entries = Seq[(String, String)]()
        users.entrySet().forEach { entry =>
          val user = entry.getKey()
          """^(\d+)\.id$""".r.findFirstMatchIn(user) match {
            case Some(m) => 
              entries = entries :+ (m.group(1), users.getString(user))
            case None =>
          }
        }
        val userMap = BiMap(entries)
        println(userMap)
      }
    }
}
