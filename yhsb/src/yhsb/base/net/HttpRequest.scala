package yhsb.base.net

import java.io.ByteArrayOutputStream

import yhsb.base.io.AutoClose.use

class HttpRequest(
    val path: String,
    val method: String,
    val charset: String
) {
  val header = new HttpHeader()
  val body = new ByteArrayOutputStream(512)

  def addHeader(header: HttpHeader) = this.header.addAll(header)

  def addHeader(key: String, value: String) = {
    header.add(key, value)
    this
  }

  def addBody(content: String) = body.writeBytes(content.getBytes(charset))

  private implicit def stringToBytes(s: String) = s.getBytes(charset)

  def getBytes: Array[Byte] = {
    use(new ByteArrayOutputStream(512)) { buf =>
      buf.write(s"$method $path HTTP/1.1\r\n")

      for ((k, v) <- header) {
        buf.write(s"$k:$v\r\n")
      }

      if (body.size() > 0) {
        buf.write(s"content-length: ${body.size()}\r\n")
      }

      buf.write("\r\n")

      if (body.size() > 0) {
        body.writeTo(buf)
      }

      buf.toByteArray
    }
  }
}
