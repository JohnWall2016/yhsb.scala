package yhsb

import utest._
import net.HttpSocket
import util.AutoClose.use

object NetTest extends TestSuite {
  def tests = Tests {
    test("HttpSocket") {
      use(new HttpSocket("115.238.190.240", 80)) { sock =>
        println(sock.getHttp("/"))
        println("你好，Scala")
      }
    }
  }
}