package yhsb.cjb.net

import java.{util => ju}

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.classTag

import com.google.gson.reflect.TypeToken
import yhsb.base.io.AutoClose
import yhsb.base.json.Json
import yhsb.base.json.Json.JsonName
import yhsb.base.json.Jsonable
import yhsb.base.net.HttpRequest
import yhsb.base.net.HttpSocket
import yhsb.base.util.Config
import yhsb.cjb.net.protocol._
import yhsb.base.struct.ListField

class Session(
    host: String,
    port: Int,
    private val userID: String,
    private val password: String
) extends HttpSocket(host, port) {
  private val cookies = mutable.Map[String, String]()

  def createRequest: HttpRequest = {
    val request = new HttpRequest("/hncjb/reports/crud", "POST")
    request
      .addHeader("Host", url)
      .addHeader("Connection", "keep-alive")
      .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
      .addHeader("Origin", s"http://$url")
      .addHeader("X-Requested-With", "XMLHttpRequest")
      .addHeader(
        "User-Agent",
        "Mozilla/5.0 (Windows NT 5.1) " +
          "AppleWebKit/537.36 (KHTML, like Gecko) " +
          "Chrome/39.0.2171.95 Safari/537.36"
      )
      .addHeader("Content-Type", "multipart/form-data;charset=UTF-8")
      .addHeader("Referer", s"http://$url/hncjb/pages/html/index.html")
      .addHeader("Accept-Encoding", "gzip, deflate")
      .addHeader("Accept-Language", "zh-CN,zh;q=0.8")
    if (!cookies.isEmpty) {
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

  def request(content: String) = {
    val request = buildRequest(content)
    write(request.getBytes())
  }

  def toService(req: Request): String = {
    val service = new JsonService(req, userID, password)
    service.toString()
  }

  def toService(id: String): String = toService(new Request(id))

  def sendService(req: Request) = request(toService(req))

  def sendService(id: String) = request(toService(id))

  def fromJson[T <: Jsonable: ClassTag](json: String): Result[T] =
    Result.fromJson(json)

  def getResult[T <: Jsonable: ClassTag](): Result[T] = {
    val result = readBody()
    // println(s"getResult: $result")
    Result.fromJson(result)
  }

  def login(): String = {
    sendService("loadCurrentUser")

    val header = readHeader()
    if (header.contains("set-cookie")) {
      val re = "([^=]+?)=(.+?);".r
      header("set-cookie").foreach(cookie => {
        re.findFirstMatchIn(cookie)
          .foreach(m => cookies(m.group(1)) = m.group(2))
      })
    }
    readBody(header)

    sendService(SysLogin(userID, password))
    readBody()
  }

  def logout(): String = {
    sendService("syslogout")
    readBody()
  }
}

object Session {
  def use[T](user: String = "002", autoLogin: Boolean = true)(
      f: Session => T
  ): T = {
    val config = Config.load("cjb.session")
    val usr = config.getConfig(s"users.$user")
    AutoClose.use(
      new Session(
        config.getString("host"),
        config.getInt("port"),
        usr.getString("id"),
        usr.getString("pwd")
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

class Request(@transient val id: String) extends Jsonable

class PageRequest(
    id: String,
    val page: Int = 1,
    @JsonName("pagesize") val pageSize: Int = 15,
    sortOpts : ju.Map[String, String] = null,
    totalOpts: ju.Map[String, String] = null,
    filterOpts: ju.Map[String, String] = null,
) extends Request(id) {
  val filtering = ju.List.of[AnyRef]()
  
  val sorting = if (sortOpts == null) {
    ju.List.of[AnyRef]()
  } else {
    ju.List.of(sortOpts)
  }
  
  val totals = if (totalOpts == null) {
    ju.List.of[AnyRef]()
  } else {
    ju.List.of(totalOpts)
  }
}

class JsonService[T <: Request](
    param: T,
    userID: String,
    password_ : String
) extends Jsonable {
  @JsonName("serviceid")
  val serviceID = param.id

  val target = ""

  @JsonName("sessionid")
  var sessionID: String = null

  @JsonName("loginname")
  val loginName: String = userID

  val password: String = password_

  private val params: T = param

  private val datas = ju.List.of(param)
}

class Result[T <: Jsonable] extends Iterable[T] with Jsonable {
  val rowcount = 0
  val page = 0
  val pagesize = 0
  val serviceid: String = null

  @JsonName("type")
  val typeOf: String = null

  val vcode: String = null
  val message: String = null
  val messagedetail: String = null

  @JsonName("datas")
  private val data = ListField[T]()

  def apply(index: Int) = data(index)

  override def size = data.size

  override def iterator = data.iterator
}

object Result {
  def fromJson[T <: Jsonable: ClassTag](json: String): Result[T] = {
    val typeOf = TypeToken
      .getParameterized(classOf[Result[_]], classTag[T].runtimeClass)
      .getType()
    Json.fromJson(json, typeOf)
  }
}

