package yhsb

import utest._
import net.HttpSocket
import base.io.AutoClose.use

object NetTest extends TestSuite {
  def tests = Tests {
    test("HttpSocket") {
      use(new HttpSocket("115.238.190.240", 80)) { sock =>
        println(sock.getHttp("/"))
      }
    }
  }
}