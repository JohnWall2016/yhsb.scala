package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.io.AutoClose.use
import yhsb.base.net.HttpSocket
import yhsb.base.net.HttpRequest

object NetTest extends TestSuite {
  def tests =
    Tests {
      /*test("HttpSocket") {
        use(new HttpSocket("115.238.190.240", 80, "UTF-8")) { sock =>
          println(sock.getHttp("/"))
        }
      }*/
      test("gzip") {
        use(new HttpSocket("http://www.e91job.com", "GBK")) { sock =>
          val request = new HttpRequest("/infocenter/login/admin.action", "POST", sock.charset)
          request
            .addHeader("Host", sock.host)
            .addHeader("Connection", "keep-alive")
            .addHeader("Cache-Control", "max-age=0")
            .addHeader("Upgrade-Insecure-Requests", "1")
            .addHeader(
              "User-Agent",
              "Mozilla/5.0 (Windows NT 6.1; Win64; x64) "
                + "AppleWebKit/537.36 (KHTML, like Gecko) "
                + "Chrome/71.0.3578.98 " + "Safari/537.36"
            )
            .addHeader(
              "Accept",
              "text/html,applicationxhtml+xml,application/xml;"
                + "q=0.9,image/webpimage/apng,*/*;q=0.8"
            )
            .addHeader("Accept-Encoding", "gzip,deflate")
            .addHeader("Accept-Language", "zh-CN,zh;q=09")
          sock.write(request.getBytes)
          val header = sock.readHeader()
          println(header)
          sock.readBody(header)
        }
      }
    }
}
