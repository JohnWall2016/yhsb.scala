package cjb.net

import yhsb.net.HttpSocket
import yhsb.net.HttpRequest
import yhsb.util.json.Jsonable
import com.google.gson.annotations.SerializedName
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.classTag
import com.google.gson.reflect.TypeToken
import yhsb.util.json.Json

class Session(
    host: String,
    port: Int,
    private val userID: String,
    private val password: String
) extends HttpSocket(host, port) {
  private val cookies = Map[String, String]()

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

}

case class Request(@transient id: String) extends Jsonable

class JsonService[T <: Request](param: T) extends Jsonable {
  @SerializedName("serviceid")
  val serviceID = param.id

  val target = ""

  @SerializedName("sessionid")
  var sessionID: String = null

  @SerializedName("loginname")
  var loginName: String = null

  var password: String = null

  private val params: T = param

  private var datas = List(params)
}

class Result[T <: Jsonable] extends Jsonable() with Iterable[T] {
  var rowcount = 0
  var page = 0
  var pagesize = 0
  var serviceid: String = null

  @SerializedName("type")
  var typeOf: String = null

  var vcode: String = null
  var message: String = null
  var messagedetail: String = null

  @SerializedName("datas")
  private var data = mutable.ListBuffer[T]()

  def add(d: T) = data.append(d)

  def apply(index: Int) = data(index)

  override def size = data.size

  override def isEmpty = size == 0

  def iterator = data.iterator

  def fromJson[T <: Jsonable: ClassTag](json: String): Result[T] = {
    val typeOf = TypeToken
      .getParameterized(classOf[Result[_]], classTag[T].runtimeClass)
      .getType()
    Json.fromJson(json, typeOf)
  }
}
