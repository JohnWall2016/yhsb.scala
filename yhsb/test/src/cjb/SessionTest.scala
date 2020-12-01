package yhsb.cjb

import utest._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol._

object SessionTest extends TestSuite {
  def tests = Tests {
    /*test("login") {
      Session.use(autoLogin = false) { sess =>
        println("=" * 50)
        println(sess.login())
        println("=" * 50)
        println(sess.logout())
      }
    }
    test("cbxx") {
      Session.use() { sess =>
        sess.sendService(CbxxRequest("430311194511291027"))
        val result = sess.getResult[Cbxx]()
        println(result)
        result.foreach { cbxx =>
          println(cbxx)
          println(s"${cbxx.cbState} ${cbxx.jfState} ${cbxx.jbKind} ${cbxx.jbState}")
          println(cbxx.czName)
          println(Xzqh.getDwName(cbxx.czName))
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
    }*/
    test("jfcx") {
      Session.use() { sess =>
        //sess.sendService(JfxxQuery("430302197604224525"))
        sess.sendService(JfxxQuery("110108196511289010"))
        val result = sess.getResult[Jfxx]()
        result.foreach(println(_))
      }
    }
  }
}

