package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.io.Repl
import requests.Session
import yhsb.cjb.net.{Session => CjbSession}

object RequestsTest extends TestSuite {
  def tests =
    Tests {
      test("run") {
        val s = Session()
        Repl(
        { args =>
          println(args)
          true
        }, 
        { it =>
          CjbSession.use() { s =>
            println(s)
          }
        }).runLoop()
      }
    }
}