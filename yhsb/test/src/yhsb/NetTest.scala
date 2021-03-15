package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.io.AutoClose.use
import yhsb.base.net.HttpSocket

object NetTest extends TestSuite {
  def tests =
    Tests {
      test("HttpSocket") {
        use(new HttpSocket("115.238.190.240", 80)) { sock =>
          println(sock.getHttp("/"))
        }
      }
    }
}
