package yhsb.base.net

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.URL
import java.nio.file.Files
import java.util.zip.GZIPInputStream

import scala.io.Source

import yhsb.base.io.AutoClose.use
import yhsb.base.net.HttpHeader
import yhsb.base.net.HttpRequest
import yhsb.base.util._
import java.net.InetSocketAddress

class HttpSocket private (
    val ip: String,
    val port: Int,
    val host: String,
    val charset: String
) extends Closeable {

  def this(
      ip: String,
      port: Int,
      charset: String
  ) =
    this(ip, port, s"$ip:$port", charset)

  def this(url: URL, charset: String) = this(
    InetAddress.getByName(url.getHost).getHostAddress(),
    url.getPort.let {
      _ match {
        case -1   => url.getDefaultPort()
        case port => port
      }
    },
    url.getHost.let { host =>
      url.getPort match {
        case -1   => host
        case port => s"$host:$port"
      }
    },
    charset
  )

  def this(url: String, charset: String) =
    this(new URL(url), charset)

  private val socket = new Socket(ip, port)
  private val input = socket.getInputStream
  private val output = socket.getOutputStream

  override def close(): Unit = {
    output.close()
    input.close()
    socket.close()
  }

  def setTimeOut(timeOut: Int) = socket.setSoTimeout(timeOut)

  def getTimeOut() = socket.getSoTimeout()

  def setKeepAlive(on: Boolean) = socket.setKeepAlive(on)

  def getKeepAlive() = socket.getKeepAlive()

  def isClosed() = socket.isClosed()

  def reconnect() = socket.connect(new InetSocketAddress(ip, port))

  def write(bytes: Array[Byte]) = output.write(bytes)

  def write(content: String): Unit = write(content.getBytes(charset))

  def readLine(): String = {
    use(new ByteArrayOutputStream(512)) { buf =>
      var continue = true
      while (continue) {
        val ch = input.read()
        //println(ch)
        ch match {
          case -1 => continue = false
          case 0xd =>
            input.read() match {
              case 0xa => continue = false
              case -1 =>
                buf.write(0xd)
                continue = false
              case ch =>
                buf.write(0xd)
                buf.write(ch)
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
      //println(line)
      if (line == null || line.isEmpty) {
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

  private def transferTo(out: OutputStream, len: Int) = {
    out.write(input.readNBytes(len))
  }

  protected def readByteArrayOutputStream(
      header: HttpHeader
  ): ByteArrayOutputStream = {
    val out = new ByteArrayOutputStream(512)
    if ("chunked" in header.get("Transfer-Encoding")) {
      var continue = true
      while (continue) {
        val len = Integer.parseInt(readLine(), 16)
        if (len <= 0) {
          readLine()
          continue = false
        } else {
          transferTo(out, len)
          readLine()
        }
      }
    } else if (header.contains("Content-Length")) {
      val len = Integer.parseInt(header.get("Content-Length").get.head, 10)
      if (len > 0) {
        transferTo(out, len)
      }
    } else {
      throw new NotImplementedError(s"unsupported transfer mode: $header")
    }
    out
  }

  protected def extractBody(
      bytes: ByteArrayOutputStream,
      header: HttpHeader
  ) = {
    use(bytes) { buf =>
      if ("gzip" in header.get("Content-Encoding")) {
        Source
          .fromInputStream(
            new GZIPInputStream(
              new ByteArrayInputStream(buf.toByteArray())
            ),
            charset
          )
          .mkString
      } else {
        buf.toString(charset)
      }
    }
  }

  def readBody(header: HttpHeader = null): String = {
    val header_ = if (header == null) readHeader() else header
    extractBody(readByteArrayOutputStream(header_), header_)
  }

  def exportToFile(fileName: String, header: HttpHeader = null) = {
    val header_ = if (header == null) readHeader() else header
    use(readByteArrayOutputStream(header_)) { buf =>
      use(new FileOutputStream(fileName)) { out =>
        if ("gzip" in header_.get("Content-Encoding")) {
          new GZIPInputStream(
            new ByteArrayInputStream(buf.toByteArray())
          ).transferTo(out)
        } else {
          buf.writeTo(out)
        }
      }
    }
  }

  def getHttp(path: String): String = {
    val request = new HttpRequest(path, "GET", "UTF-8")
    request
      .addHeader("Host", host)
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
    write(request.getBytes)
    readBody()
  }
}
