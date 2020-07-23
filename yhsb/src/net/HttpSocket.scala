package yhsb.net

import java.net.Socket
import yhsb.util.AutoClose.use
import java.io.ByteArrayOutputStream
import yhsb.net.HttpHeader
import java.io.InputStream
import java.io.OutputStream
import yhsb.net.HttpRequest
import java.io.Closeable

class HttpSocket(
    val host: String,
    val port: Int,
    val charset: String = "UTF-8"
) extends Closeable {

  val url = s"$host:$port"

  private val socket = new Socket(host, port)
  private val input = socket.getInputStream()
  private val output = socket.getOutputStream()

  override def close(): Unit = {
    output.close()
    input.close()
    socket.close()
  }

  def write(bytes: Array[Byte]) = output.write(bytes)

  def write(content: String): Unit = write(content.getBytes(charset))

  def readLine(): String = {
    use(new ByteArrayOutputStream(512)) { buf =>
      var continue = true
      while (continue) {
        input.read() match {
          case -1 => continue = false
          case 0xd => {
            input.read() match {
              case 0xa => continue = false
              case -1 => {
                buf.write(0xd)
                continue = false
              }
              case ch => {
                buf.write(0xd)
                buf.write(ch)
              }
            }
          }
          case ch => buf.write(ch)
        }
      }
      buf.toString(charset)
    }
  }

  def readHeader(): HttpHeader = {
    val header = new HttpHeader()
    var continue = true
    while (continue) {
      val line = readLine()
      // println(line)
      if (line == null || line.isEmpty()) {
        continue = false
      } else {
        val i = line.indexOf(":")
        if (i > 0) {
          header.add(
            line.substring(0, i).trim(),
            line.substring(i + 1).trim()
          )
        }
      }
    }
    header
  }

  private def transfer(to: OutputStream, len: Int) {
    to.write(input.readNBytes(len))
  }

  def readBody(header: HttpHeader = null): String = {
    val header_ = if (header == null) readHeader() else header
    use(new ByteArrayOutputStream(512)) { buf =>
      if (header_.get("Transfer-Encoding").exists(_.exists(_ == "chunked"))) {
        var continue = true
        while (continue) {
          val len = Integer.parseInt(readLine(), 16)
          if (len <= 0) {
            readLine()
            continue = false
          } else {
            transfer(buf, len)
            readLine()
          }
        }
      } else if (header_.contains("Content-Length")) {
        val len = Integer.parseInt(header_.get("Content-Length").get.head, 10)
        if (len > 0) {
          transfer(buf, len)
        }
      } else {
        throw new NotImplementedError("unsupported transfer mode")
      }
      buf.toString(charset)
    }
  }

  def getHttp(path: String): String = {
    val request = new HttpRequest(path, "GET");
    request
      .addHeader("Host", url)
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
    write(request.getBytes())
    readBody()
  }
}
