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
import org.apache.axis.encoding.Base64
import java.nio.file.Files
import yhsb.base.run.process
import java.io.OutputStream
import yhsb.base.json.Json

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
    private var userID: String,
    private var password: String,
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
      .addHeader("X-Requested-With", "XMLHttpRequest")
      .addHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 6.1; Win64; x64) " +
          "AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/92.0.4515.131 Safari/537.36"
      )
      .addHeader("Content-Type", "multipart/form-data;charset=UTF-8")
      .addHeader("Origin", s"http://$host")
      .addHeader("Referer", s"http://$host/hncjb/pages/html/index.html")
      .addHeader("Accept-Encoding", "gzip, deflate")
      .addHeader("Accept-Language", "zh-CN,zh;q=0.9")
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

  def sendService(req: Request[_], userID: String, password: String) =
    writeRequest(toService(req, userID, password))

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

  def getResult[T: ClassTag](header: HttpHeader): Result[T] = {
    val result = readBody(header)
    // println(s"getResult: $result")
    Result.fromJson(result)
  }

  def request[T: ClassTag](reqWithTag: Request[T]): Result[T] = {
    sendService(reqWithTag)
    getResult[T]
  }

  def request[T: ClassTag](
      reqWithTag: Request[T],
      userID: String,
      password: String
  ): Result[T] = {
    sendService(reqWithTag, userID, password)
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

  private def setCookies(header: HttpHeader) = {
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
  }

  def login(): String = {
    sendService("contentQuery", null, null)

    val header = readHeader()
    setCookies(header)
    readBody(header)

    sendService(SysLogin(userID + "|@|1", password))
    val result = readBody()
    val loginResult = Result.fromJson[Any](result)

    if (loginResult.typeOf == "error") {
      throw new Exception(s"error: ${loginResult.message}")
    }

    result
  }

  def loginSSCard(checkTimes: Int = 6, delay: Long = 6 * 1000): String = {
    sendService("getQrCode", null, null)
    val header = readHeader()
    setCookies(header)

    val qrCodeResult = getResult[String](header)
    println(qrCodeResult)

    if (qrCodeResult.size < 3) {
      throw new Exception("get QrCode error")
    }

    Session.openBase64Jpg(qrCodeResult(0))

    var succeed = false
    var times = 1
    var result = ""
    while (times <= checkTimes && succeed == false) {
      val checkResult =
        request(CheckQrCode(qrCodeResult(1), qrCodeResult(2)), null, null)

      if (checkResult.typeOf == "info" && checkResult.size > 0) {
        val userName = checkResult(0)
        println(s"sscard authorized: $userName")

        sendService(
          SysLogin(userName + "|@|0", Config.cjbSession.getString("sscard.pwd"))
        )

        val result = readBody()

        val loginResult = Result.fromJson[SysLogin.Item](result)

        if (loginResult.typeOf == "data" && loginResult.size > 0) {
          val userName = loginResult(0).userName
          val userPass = loginResult(0).password
          if (userName != null && userPass != null) {
            userID = userName
            password = userPass
            succeed = true
          }
        }
        if (!succeed) {
          if (times < checkTimes) {
            println(s"error: ${loginResult.message}")
          } else {
            throw new Exception(s"error: ${loginResult.message}")
          }
        }
      } else {
        if (times < checkTimes) {
          println(s"error: ${checkResult.message}")
        } else {
          throw new Exception(s"error: ${checkResult.message}")
        }
      }
      times += 1
      if (!succeed && times <= checkTimes) {
        Thread.sleep(delay)
      }
    }

    result
  }

  def logout(): String = {
    //sendService("syslogout")
    //readBody()
    ""
  }

  private def urlEncode(s: String) = URLEncoder.encode(s, charset)

  private def httpRequest(url: String) = {
    val request =
      new HttpRequest(url, "GET", charset)
    request
      .addHeader("Host", host)
      .addHeader("Connection", "keep-alive")
      .addHeader("Upgrade-Insecure-Requests", "1")
      .addHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 6.1; Win64; x64) " +
          "AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/92.0.4515.131 Safari/537.36"
      )
      .addHeader(
        "Accept",
        "text/html,application/xhtml+xml," +
          "application/xml;q=0.9,image/avif,image/webp," +
          "image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
      )
      .addHeader("Referer", s"http://$host/hncjb/pages/html/index.html")
      .addHeader("Accept-Encoding", "gzip, deflate")
      .addHeader("Accept-Language", "zh-CN,zh;q=0.9")

    if (cookies.nonEmpty) {
      request.addHeader(
        "Cookie",
        cookies.map(e => s"${e._1}=${e._2}").mkString("; ")
      )
    }

    request
  }

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

    write(httpRequest(url).getBytes)
    exportToFile(filePath)
  }

  def readHttp(url: String) = {
    write(httpRequest(url).getBytes)
    readBody()
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

  def getLoginedSession(
      user: String = "007",
      verbose: Boolean = false,
      loginTimeOut: Int = 6 * 1000,
      loginRetries: Int = 6,
      useSSCard: Boolean = false
  ): Session = {
    var session: Session = null
    try {
      val usr = Config.cjbSession.getConfig(s"users.$user")
      session = new Session(
        Config.cjbSession.getString("host"),
        Config.cjbSession.getInt("port"),
        usr.getString("id"),
        usr.getString("pwd"),
        usr.getString("cxcookie"),
        usr.getString("jsessionid_ylzcbp"),
        verbose
      )
      val oldValue = session.getTimeOut()
      session.setTimeOut(loginTimeOut)
      if (!useSSCard) {
        session.login()
      } else {
        session.loginSSCard()
      }
      session.setTimeOut(oldValue)
      
      val result = session
      session = null
      result
    } catch {
      case ex: java.net.SocketTimeoutException => {
        if (loginRetries > 0) {
          println(s"Logining retries ...")
          getLoginedSession(
            user,
            verbose,
            loginTimeOut,
            loginRetries - 1,
            useSSCard
          )
        } else {
          throw ex
        }
      }
    } finally {
      if (session != null) {
        session.close()
      }
    }
  }

  def use[T](
      user: String = "007",
      verbose: Boolean = false,
      loginTimeOut: Int = 6 * 1000,
      loginRetries: Int = 6,
      useSSCard: Boolean = false
  )(
      f: Session => T
  ): T = {
    if (globalSession.isDefined) {
      f(globalSession.get)
    } else {
      AutoClose.use(
        getLoginedSession(
          user,
          verbose,
          loginTimeOut,
          loginRetries,
          useSSCard
        )
      ) { sess =>
        try {
          f(sess)
        } finally {
          sess.logout()
        }
      }
    }
  }

  var globalSession: Option[Session] = None

  def setGlobalSession(session: Option[Session]): String = {
    if (globalSession.isDefined) {
      println(s"Close global sesion: ${globalSession.get.userID}")
      globalSession.get.logout()
      globalSession.get.close()
      globalSession = None
    }

    globalSession = session

    if (session.isDefined) {
      val user = session.get.userID
      println(s"Set global sesion: $user")
      user
    } else ""
  }

  def openBase64Jpg(base64String: String) = {
    val imgBytes = Base64.decode(base64String.replace("\\n", ""))

    val file = Files.createTempFile("yhsb_login", ".jpg")
    Files.write(file, imgBytes)

    val cmd =
      s"""${yhsb.base.util.Config.load("cmd").getString("open.img")} $file"""

    println(cmd)
    process.execute(
      cmd,
      OutputStream.nullOutputStream(),
      OutputStream.nullOutputStream()
    )
  }
}
