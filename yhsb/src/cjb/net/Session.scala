package yhsb.cjb.net

import yhsb.net.HttpSocket
import yhsb.net.HttpRequest
import yhsb.util.json.Jsonable
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.classTag
import com.google.gson.reflect.TypeToken
import yhsb.util.json.Json
import yhsb.util.json.Json.JsonName
import java.{util => ju}

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
    if (cookies.isEmpty) {
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

  def fromJson[T<: Jsonable: ClassTag](json: String): Result[T] = Result.fromJson(json)

  def getResult[T<: Jsonable: ClassTag](): Result[T] = {
    Result.fromJson(readBody())
  }

  def login(): String = {
    sendService("loadCurrentUser")

    val header = readHeader()
    if (header.contains("set-cookie")) {
      val re = "([^=]+?)=(.+?);".r
      header("set-cookie").foreach(cookie => {
        re.findFirstMatchIn(cookie).foreach(
          m => cookies(m.group(1)) = m.group(2)
        )
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

class Request(@transient val id: String) extends Jsonable

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

class Result[T <: Jsonable] extends Jsonable() with Iterable[T] {
  var rowcount = 0
  var page = 0
  var pagesize = 0
  var serviceid: String = null

  @JsonName("type")
  var typeOf: String = null

  var vcode: String = null
  var message: String = null
  var messagedetail: String = null

  @JsonName("datas")
  private var data = mutable.ListBuffer[T]()

  def add(d: T) = data.append(d)

  def apply(index: Int) = data(index)

  override def size = data.size

  override def isEmpty = size == 0

  def iterator = data.iterator

}

object Result {
  def fromJson[T<: Jsonable: ClassTag](json: String): Result[T] = {
    val typeOf = TypeToken
      .getParameterized(classOf[Result[_]], classTag[T].runtimeClass)
      .getType()
    Json.fromJson(json, typeOf)
  }
}

case class SysLogin(
    @JsonName("username") val userName: String,
    @JsonName("passwd") val password: String
) extends Request("syslogin")
