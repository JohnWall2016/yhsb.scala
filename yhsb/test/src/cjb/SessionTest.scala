package yhsb.cjb

import utest._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol._

object SessionTest extends TestSuite {
  def tests = Tests {
    test("login") {
      Session.use(autoLogin = false) { sess =>
        println("=" * 50)
        println(sess.login())
        println("=" * 50)
        println(sess.logout())
      }
    }
    test("cbxx") {
      Session.use() { sess =>
        sess.sendService(CbxxRequest("520113195504010432"))
        val result = sess.getResult[Cbxx]()
        println(result)
        result.foreach { cbxx =>
          println(cbxx)
          println(s"${cbxx.cbState} ${cbxx.jfState} ${cbxx.jbKind} ${cbxx.jbState}")
        }
      }
    }
    test("cbsh") {
      Session.use() { sess =>
        sess.sendService(CbshQuery("2020-07-20"))
        val result = sess.getResult[Cbsh]()
        result.foreach { cbsh =>
          println(cbsh)
        }
      }
    }
  }
}

