package yhsb.cjb.net

import java.net.URLEncoder

import scala.collection.SeqMap
import scala.collection.mutable
import scala.reflect.ClassTag

import yhsb.base.io.AutoClose
import yhsb.base.json.Jsonable
import yhsb.base.net.HttpHeader
import yhsb.base.net.HttpRequest
import yhsb.base.net.HttpSocket
import yhsb.cjb.db.CjbSession
import yhsb.cjb.net.protocol._

object Config {
  val cjbSession = yhsb.base.util.Config.load("cjb.session")

  def getSession(userID: String) = {
    import yhsb.cjb.db.YhsbDB._
    val session: List[CjbSession] = run(
      cjbSessionData.filter(_.user == lift(userID))
    )
    if (session.isEmpty) None
    else Some(session(0))
  }

  def updateSession(userID: String, cxcookie: String, jsessionid: String) = {
    import yhsb.cjb.db.YhsbDB._
    val session: List[CjbSession] = run(
      cjbSessionData.filter(_.user == lift(userID))
    )
    if (session.isEmpty) {
      run(
        cjbSessionData.insert(
          CjbSession(lift(userID), Some(lift(cxcookie)), Some(lift(jsessionid)))
        )
      )
    } else {
      run(
        cjbSessionData
          .filter(_.user == lift(userID))
          .update(
            _.cxcookie -> Some(lift(cxcookie)),
            _.jsessionid -> Some(lift(jsessionid))
          )
      )
    }
  }
}

class Session(
    ip: String,
    port: Int,
    private val userID: String,
    private val password: String,
    private val cxcookie: String,
    private val jsessionid_ylzcbp: String,
    private val verbose: Boolean = false
) extends HttpSocket(ip, port, "UTF-8") {
  private val cookies = {
    val sess = Config.getSession(userID)
    mutable.Map(
      "cxcookie" -> {
        sess
          .flatMap(_.cxcookie)
          .getOrElse(cxcookie)
      },
      "jsessionid_ylzcbp" -> {
        sess
          .flatMap(_.jsessionid)
          .getOrElse(jsessionid_ylzcbp)
      }
    )
  }

  def createRequest: HttpRequest = {
    val request = new HttpRequest("/hncjb/reports/crud", "POST", charset)
    request
      .addHeader("Host", host)
      .addHeader("Connection", "keep-alive")
      .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
      .addHeader("Origin", s"http://$host")
      .addHeader("X-Requested-With", "XMLHttpRequest")
      .addHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 5.1) " +
          "AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/39.0.2171.95 Safari/537.36"
      )
      .addHeader("Content-Type", "multipart/form-data;charset=UTF-8")
      .addHeader("Referer", s"http://$host/hncjb/pages/html/index.html")
      .addHeader("Accept-Encoding", "gzip, deflate")
      .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
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
    if (verbose) {
      println(s"buildRequest(head): ${request.header}\r\n")
      println(s"buildRequest(body): ${content}\r\n")
    }
    request
  }

  def writeRequest(content: String) = {
    val request = buildRequest(content)
    write(request.getBytes)
  }

  def toService(req: Request[_]): String = {
    val service = new JsonService(req, userID, password)
    val result = service.toString()
    result
  }

  def toService(req: Request[_], userID: String, password: String): String = {
    val service = new JsonService(req, userID, password)
    val result = service.toString()
    result
  }

  def toService(id: String): String = toService(new Request(id))

  def toService(id: String, userID: String, password: String): String = {
    toService(new Request(id), userID, password)
  }

  def sendService(req: Request[_]) = writeRequest(toService(req))

  def sendService(id: String) = writeRequest(toService(id))

  def sendService(id: String, userID: String, password: String) = {
    writeRequest(toService(id, userID, password))
  }

  def fromJson[T: ClassTag](json: String): Result[T] =
    Result.fromJson(json)

  override def readBody(header: HttpHeader): String = {
    val header_ = if (header == null) readHeader() else header
    val body = extractBody(readByteArrayOutputStream(header_), header_)
    if (verbose) {
      println(s"readBody(head): ${header_}\r\n")
      println(s"readBody(body): ${body}\r\n")
    }
    body
  }

  def getResult[T: ClassTag]: Result[T] = {
    val result = readBody()
    // println(s"getResult: $result")
    Result.fromJson(result)
  }

  def request[T: ClassTag](reqWithTag: Request[T]): Result[T] = {
    sendService(reqWithTag)
    getResult[T]
  }

  def requestOr[T: ClassTag](req1: Request[T], req2: Request[T]) = {
    val ret = request(req1)
    if (ret.nonEmpty) {
      ret
    } else {
      request(req2)
    }
  }

  def login(): String = {
    sendService("contentQuery", null, null)

    val header = readHeader()
    if (header.contains("set-cookie")) {
      println(s"${cookies("cxcookie")}|${cookies("jsessionid_ylzcbp")}")
      val re = "([^=]+?)=(.+?);".r
      header("set-cookie").foreach(cookie => {
        re.findFirstMatchIn(cookie)
          .foreach(m => cookies(m.group(1)) = m.group(2))
      })
      println(s"${cookies("cxcookie")}|${cookies("jsessionid_ylzcbp")}")
      Config.updateSession(
        userID,
        cookies("cxcookie"),
        cookies("jsessionid_ylzcbp")
      )
    }
    readBody(header)

    sendService(SysLogin(userID + "|@|1", password))
    readBody()
  }

  def logout(): String = {
    //sendService("syslogout")
    //readBody()
    ""
  }

  private def urlEncode(s: String) = URLEncoder.encode(s, charset)

  def exportTo(
      req: PageRequest[_],
      columnHeaders: SeqMap[String, String]
  )(
      filePath: String
  ) = {
    val args = mutable.LinkedHashMap[String, String]()
    args("serviceid") = "exportexecl"
    args("queryserviceid") = req.id
    args("params") = urlEncode(req.toJsonNoNulls)
    args("columns") = urlEncode(
      columnHeaders
        .map { case (key, value) =>
          s"""{"dataKey":"$key","headerText":"$value"}"""
        }
        .toSeq
        .mkString("[", ",", "]")
    )

    val url = "/hncjb/reports/crud?" + args
      .map { case (key, value) =>
        s"$key=$value"
      }
      .mkString("&")

    val request =
      new HttpRequest(url, "GET", charset)
    request
      .addHeader("Host", host)
      .addHeader("Connection", "keep-alive")
      .addHeader("Upgrade-Insecure-Requests", "1")
      .addHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 5.1) " +
          "AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/39.0.2171.95 Safari/537.36"
      )
      .addHeader(
        "Accept",
        "text/html,application/xhtml+xml," +
          "application/xml;q=0.9,image/avif,image/webp," +
          "image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
      )
      .addHeader("Referer", s"http://$host/hncjb/pages/html/index.html")
      .addHeader("Accept-Encoding", "gzip, deflate")
      .addHeader("Accept-Language", "zh-CN,zh;q=0.8")

    if (cookies.nonEmpty) {
      request.addHeader(
        "Cookie",
        cookies.map(e => s"${e._1}=${e._2}").mkString("; ")
      )
    }
    write(request.getBytes)
    exportToFile(filePath)
  }

  def exportAllTo(
      req: PageRequest[_],
      columnHeaders: SeqMap[String, String]
  )(
      filePath: String
  ) = {
    import yhsb.base.util._

    exportTo(
      req.set { it =>
        it.page = 1
        it.pageSize = null
      },
      columnHeaders
    )(
      filePath
    )
  }
}

object Session {
  def use[T](user: String = "002", autoLogin: Boolean = true, verbose: Boolean = true)(
      f: Session => T
  ): T = {
    val usr = Config.cjbSession.getConfig(s"users.$user")
    AutoClose.use(
      new Session(
        Config.cjbSession.getString("host"),
        Config.cjbSession.getInt("port"),
        usr.getString("id"),
        usr.getString("pwd"),
        usr.getString("cxcookie"),
        usr.getString("jsessionid_ylzcbp"),
        verbose
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
