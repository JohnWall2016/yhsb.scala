package yhsb.cjb.net.protocol

import com.google.gson.reflect.TypeToken
import yhsb.base.json.Json
import yhsb.base.struct.ListField

import java.{util => ju}
import scala.reflect.{ClassTag, classTag}
import scala.collection.immutable.AbstractSeq

class Request[T : ClassTag](@transient val id: String) extends Jsonable

class PageRequest[T : ClassTag](
    id: String,
    val page: Int = 1,
    @JsonName("pagesize") var pageSize: Integer = 15,
    sortOptions : Map[String, String] = null,
    totalOptions: Map[String, String] = null,
    filterOptions: Map[String, String] = null,
) extends Request(id) {
  def initOptions(options: Map[String, String]) = {
    import scala.jdk.CollectionConverters._
    if (options == null) {
      ju.List.of()
    } else {
      ju.List.of(options.asJava)
    }
  }

  val filtering = initOptions(filterOptions)
  
  val sorting = initOptions(sortOptions)
  
  val totals = initOptions(totalOptions)
}

class JsonService[T <: Request[_]](
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

class Result[T] extends AbstractSeq[T] with Jsonable {
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

  override def length = data.length

  override def iterator = data.iterator
}

object Result {
  def fromJson[T : ClassTag](json: String): Result[T] = {
    val typeOf = TypeToken
      .getParameterized(classOf[Result[_]], classTag[T].runtimeClass)
      .getType
    Json.fromJson(json, typeOf)
  }
}

