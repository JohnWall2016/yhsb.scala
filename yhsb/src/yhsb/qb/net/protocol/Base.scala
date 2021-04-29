package yhsb.qb.net.protocol

import org.w3c.dom.Document
import org.w3c.dom.Element

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import scala.collection.mutable.LinkedHashMap

import yhsb.base.xml._
import yhsb.base.text.String._

class Request[T: TypeTag: ClassTag](
    @transient val funID: String
) {
  type Item = T
}

case class EmptyItem()

class System(funID: String) {
  @AttrNode("para", "usr")
  var user: String = ""

  @AttrNode("para", "pwd")
  var password: String = ""

  @AttrNode("para", "funid")
  val funID_ : String = funID
}

class InHeader(funID: String) {
  @Namespaces("in" -> "http://www.molss.gov.cn/")
  @Node("in:system")
  var system = new System(funID)
}

class InBody[T](
    @Namespaces("in" -> "http://www.molss.gov.cn/")
    @Node("in:business")
    val business: T
)

@Namespaces("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")
@Node("soap:Envelope")
case class InEnvelope[T <: Request[_]](
    params: T
) {
  @Attribute("soap:encodingStyle")
  val encodingStyle = "http://schemas.xmlsoap.org/soap/encoding/"

  @Node("soap:Header")
  val header = new InHeader(params.funID)

  @Node("soap:Body")
  val body = new InBody(params)

  def user = header.system.user

  def user_=(value: String) = {
    header.system.user = value
  }

  def password = header.system.password

  def password_=(value: String) = {
    header.system.password = value
  }
}

case class OutResult(
    @Attribute("sessionID")
    var sessionID: String,
    @Attribute("message")
    var message: String
)

case class OutHeader(
    var result: OutResult
)

case class OutBusiness[T](
    @AttrNode("result", "result")
    var result: String,
    @AttrNode("result", "row_count")
    var rowCount: Int,
    @AttrNode("result", "querysql")
    var querySql: String,
    @Node("resultset", OutBusiness.filter)
    var _resultSet: ResultSet[T],
    @Node("resultset", OutBusiness.nofilter)
    var otherResultSets: List[ResultSet[T]]
) {
  def resultSet = if (_resultSet == null) ResultSet.empty else _resultSet
}

object OutBusiness {
  val filter: Element => Boolean = { e =>
    "^querylist|cxjg$".r.matches(e.getAttribute("name"))
  }

  val nofilter: Element => Boolean = { e =>
    !"^querylist|cxjg$".r.matches(e.getAttribute("name"))
  }
}

case class OutBody[T](
    @Namespaces("out" -> "http://www.molss.gov.cn/")
    @Node("out:business")
    var result: OutBusiness[T]
)

@Namespaces("soap" -> "http://schemas.xmlsoap.org/soap/envelope/")
@Node("soap:Envelope")
case class OutEnvelope[T](
    @Attribute("soap:encodingStyle")
    var encodingStyle: String,
    @Node("soap:Header")
    var header: OutHeader,
    @Node("soap:Body")
    var body: OutBody[T]
)

case class ResultSet[T](
    @Attribute("name")
    var name: String,
    @Node("row")
    var rowList: List[T]
) extends Iterable[T] {
  override def iterator: Iterator[T] = {
    rowList.iterator
  }
}

object ResultSet {
  val empty = ResultSet[Nothing](null, List())
}

case class Result(
    @Attribute("result")
    var result: String,
    @Attribute("row_count")
    var rowCount: Int,
    @Attribute("querysql")
    var querySql: String
)

class FunctionID[T: TypeTag: ClassTag](funID: String, functionID: String)
  extends Request[T](funID) {
  @AttrNode("para", "functionid")
  val functionID_ = functionID
}

class Query(funID: String, functionID: String) extends Request(funID) {
  @AttrNode("para", "startrow")
  val startRow = "1"

  @AttrNode("para", "row_count")
  val rowCount = "-1"

  @AttrNode("para", "pagesize")
  val pageSize = "500"

  @AttrNode("para", "functionid")
  val functionID_ = functionID
}

class SimpleClientSql(funID: String, functionID: String, sql: String = "")
  extends Request(funID) {
  @AttrNode("para", "clientsql")
  val clientSql = sql

  @AttrNode("para", "functionid")
  val functionID_ = functionID
}

class ClientSql[T: TypeTag: ClassTag](
    funID: String,
    functionID: String,
    sql: String = ""
) extends Request[T](funID) {
  @AttrNode("para", "startrow")
  val startRow = "1"

  @AttrNode("para", "row_count")
  val rowCount = "-1"

  @AttrNode("para", "pagesize")
  val pageSize = "500"

  @AttrNode("para", "clientsql")
  val clientSql = sql

  @AttrNode("para", "functionid")
  val functionID_ = functionID
}

class AddSql(
    funID: String,
    fid: String,
    addSql: String,
    start: Int = 0,
    pageSize: Int = 0
) extends Request(funID) {
  @AttrNode("para", "pagesize")
  val pageSize_ = pageSize

  @AttrNode("para", "addsql")
  val addSql_ = addSql

  @AttrNode("para", "begin")
  val start_ = start

  @AttrNode("para", "fid")
  val fid_ = fid
}

class ParaList(
    val attrs: LinkedHashMap[String, String],
    val paraList: LinkedHashMap[String, String]
) extends CustomElement {
  def toElement(
      doc: Document,
      nodeName: String,
      namespaces: collection.Map[String, String]
  ): Element = {
    import yhsb.base.xml.Extension.RichDocument
    val elem = doc.createElement(
      (if (nodeName.nonNullAndEmpty) nodeName else "paralist"),
      attrs
    )
    paraList.foreach { case (key, value) =>
      elem.appendChild(doc.createElement("row", Map(key -> value)))
    }
    elem
  }
}

class ParamList(
    funID: String,
    attrs: LinkedHashMap[String, String],
    paraList: LinkedHashMap[String, String]
) extends Request(funID) {
  @Node("paralist")
  val list = new ParaList(attrs, paraList)
}
