package yhsb.qb.net

import scala.collection.mutable
import scala.reflect.runtime.universe._
import scala.reflect.ClassTag

import yhsb.base.io.AutoClose
import yhsb.base.json.Jsonable
import yhsb.base.net.HttpRequest
import yhsb.base.net.HttpSocket
import yhsb.base.xml.Extension._
import yhsb.qb.net.protocol._

object Config {
  val qbSession = yhsb.base.util.Config.load("qb.session")
}

class Session(
    ip: String,
    port: Int,
    private val userID: String,
    private val password: String,
    private val agencyCode: String,
    private val agencyName: String
) extends HttpSocket(ip, port, "GBK") {
  private val cookies = mutable.Map[String, String]()

  def createRequest: HttpRequest = {
    val request = new HttpRequest("/sbzhpt/MainServlet", "POST", charset)
    request
      .addHeader("SOAPAction", "mainservlet")
      .addHeader("Content-Type", "text/html;charset=GBK")
      .addHeader("Host", host)
      .addHeader("Connection", "keep-alive")
      .addHeader("Cache-Control", "no-cache")
    if (cookies.nonEmpty) {
      request.addHeader(
        "Cookie",
        cookies.map(e => s"${e._1}=${e._2}").mkString("; ")
      )
    }
    request
  }

  def buildRequest(content: String): HttpRequest = {
    val request = createRequest
    request.addBody(content)
    request
  }

  def writeRequest(content: String) = {
    val request = buildRequest(content)
    write(request.getBytes)
  }

  def toService[T: TypeTag](req: T): String = {
    val request =
      req.toXml(declare = """<?xml version="1.0" encoding="GBK"?>""")
    //println(s"toService: $request")
    request
  }

  def sendService[T <: Request[_]: TypeTag](req: T) = {
    val inEnv = new InEnvelope(req)
    inEnv.user = userID
    inEnv.password = password
    writeRequest(toService(inEnv))
  }

  def fromXml[T: TypeTag](xml: String): T =
    xml.toElement.toObject[T]

  def getResult[T: TypeTag: ClassTag]: OutBusiness[T] = {
    val result = readBody()
    //println(s"getResult: $result")
    val outEnv = result.toElement.toObject[OutEnvelope[T]]
    //println(s"getResult2: $outEnv")
    outEnv.body.result
  }

  def request[T <: Request[_]: TypeTag: ClassTag](
      reqWithTag: T
  ): OutBusiness[reqWithTag.Item] = {
    sendService(reqWithTag)
    getResult[reqWithTag.Item]
  }

  def login(): String = {
    sendService(new Login)

    val header = readHeader()
    if (header.contains("set-cookie")) {
      val re = "([^=]+?)=(.+?);".r
      header("set-cookie").foreach(cookie => {
        re.findFirstMatchIn(cookie)
          .foreach(m => cookies(m.group(1)) = m.group(2))
      })
    }
    readBody(header)
  }

  def logout() = {}
}

object Session {
  def use[T](user: String = "sqb", autoLogin: Boolean = true)(
      f: Session => T
  ): T = {
    val usr = Config.qbSession.getConfig(s"users.$user")
    AutoClose.use(
      new Session(
        Config.qbSession.getString("host"),
        Config.qbSession.getInt("port"),
        usr.getString("id"),
        usr.getString("pwd"),
        usr.getString("agencyCode"),
        usr.getString("agencyName")
      )
    ) { sess =>
      if (autoLogin) sess.login()
      try {
        f(sess)
      } finally {
        if (autoLogin) sess.logout()
      }
    }
  }
}
