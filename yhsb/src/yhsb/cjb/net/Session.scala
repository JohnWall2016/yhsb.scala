package yhsb.cjb.net

import scala.collection.mutable
import yhsb.cjb.net.protocol.Request
import yhsb.cjb.net.protocol.JsonService
import requests.RequestBlob
import scala.reflect.ClassTag
import yhsb.cjb.net.protocol.Result
import yhsb.cjb.net.protocol.SysLogin
import yhsb.cjb.db.CjbSession
import java.net.URLEncoder
import yhsb.cjb.net.protocol.PageRequest
import scala.collection.SeqMap

class Session(
    ip: String,
    port: Int,
    private var userID: String,
    private var password: String,
    private val cxcookie: String,
    private val jsessionid_ylzcbp: String,
    private val verbose: Boolean = false
) {
  val host = s"$ip:$port"

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

  val session = requests.Session()

  def postRequest(content: String) = {
    session.post(
      url = s"http://$host/hncjb/reports/crud",
      headers = Map(
        "Host" -> host,
        "Connection" -> "keep-alive",
        "Accept" -> "application/json, text/javascript, */*; q=0.01",
        "X-Requested-With" -> "XMLHttpRequest",
        "User-Agent" ->
          "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36",
        "Content-Type" -> "multipart/form-data;charset=UTF-8",
        "Origin" -> s"http://$host",
        "Referer" -> s"http://$host/hncjb/pages/html/index.html",
        "Accept-Encoding" -> "gzip, deflate",
        "Accept-Language" -> "zh-CN,zh;q=0.9",
        "Cookie" -> cookies.map(e => s"${e._1}=${e._2}").mkString("; ")
      ),
      data = RequestBlob.ByteSourceRequestBlob(content),
      keepAlive = true
    )
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

  def toService(req: Request[_]): String = {
    val service = new JsonService(req, userID, password)
    val result = service.toString()
    result
  }

  def sendService(req: Request[_]) = postRequest(toService(req))

  def sendService(id: String, userID: String, password: String) = {
    postRequest(toService(id, userID, password))
  }

  def request[T: ClassTag](reqWithTag: Request[T]): Result[T] = {
    val resp = sendService(reqWithTag)
    Result.fromJson(resp.text())
  }

  def requestOr[T: ClassTag](req1: Request[T], req2: Request[T]) = {
    val ret = request(req1)
    if (ret.nonEmpty) {
      ret
    } else {
      request(req2)
    }
  }

  private def setCookies(header: Map[String, Seq[String]]) = {
    if (header.contains("Set-Cookie")) {
      println(s"${cookies("cxcookie")}|${cookies("jsessionid_ylzcbp")}")
      val re = "([^=]+?)=(.+?);".r
      header("Set-Cookie").foreach(cookie => {
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
    var resp = sendService("contentQuery", null, null)
    setCookies(resp.headers)

    resp = sendService(SysLogin(userID + "|@|1", password))
    val result = resp.text()
    val loginResult = Result.fromJson[Any](result)

    if (loginResult.typeOf == "error") {
      throw new Exception(s"error: ${loginResult.message}")
    }
    result
  }

  def logout(): String = {
    //sendService("syslogout")
    ""
  }

  private def urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

  private def getRequest(
      url: String,
      readTimeOut: Int = 50 * 1000,
      connectTimeOut: Int = 50 * 1000
  ) = {
    session.get(
      url = url,
      headers = Map(
        "Host" -> host,
        "Connection" -> "keep-alive",
        "Upgrade-Insecure-Requests" -> "1",
        "Accept" -> "application/json, text/javascript, */*; q=0.01",
        "User-Agent" ->
          "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.131 Safari/537.36",
        "Accept" -> "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9",
        "Referer" -> s"http://$host/hncjb/pages/html/index.html",
        "Accept-Encoding" -> "gzip, deflate",
        "Accept-Language" -> "zh-CN,zh;q=0.9",
        "Cookie" -> cookies.map(e => s"${e._1}=${e._2}").mkString("; ")
      ),
      keepAlive = true,
      readTimeout = readTimeOut,
      connectTimeout = connectTimeOut
    )
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

    val url = s"http://$host/hncjb/reports/crud?" + args
      .map { case (key, value) =>
        s"$key=$value"
      }
      .mkString("&")

    val file = os.Path(filePath)
    if (os.exists(file)) {
      os.write.over(file, getRequest(url, readTimeOut = 0, connectTimeOut = 0))
    } else {
      os.write(file, getRequest(url, readTimeOut = 0, connectTimeOut = 0))
    }
    // println(file)
  }

  def readHttp(url: String) = {
    getRequest(url).text()
  }

  def readHttpPart(part: String) = {
    getRequest(s"http://$host$part").text()
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
  private val sessions: mutable.Map[String, Session] = mutable.Map()

  def use[T](
      user: String = "007",
      verbose: Boolean = false,
      loginTimeOut: Int = 6 * 1000,
      loginRetries: Int = 6,
      useSSCard: Boolean = false
  )(
      f: Session => T
  ): T = {
    val sess = sessions.getOrElseUpdate(
      user, {
        val usr = Config.cjbSession.getConfig(s"users.$user")
        val s = new Session(
          Config.cjbSession.getString("host"),
          Config.cjbSession.getInt("port"),
          usr.getString("id"),
          usr.getString("pwd"),
          usr.getString("cxcookie"),
          usr.getString("jsessionid_ylzcbp"),
          verbose
        )
        s.login()
        s
      }
    )
    hooks.foreach(_(user))
    f(sess)
  }

  private val hooks = mutable.ListBuffer[String => Unit]()

  def addUserChangedHooks(hook: String => Unit) = {
    hooks.addOne(hook)
  }
}

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
