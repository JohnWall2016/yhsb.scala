package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.xml._
import yhsb.base.xml.Extension._
import yhsb.base.reflect.Extension._

object Test {
  val xml = """<?xml version="1.0" encoding="GBK"?>
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" soap:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
  <soap:Header>
    <in:system xmlns:in="http://www.molss.gov.cn/">
      <para usr="abc"/>
      <para pwd="YLZ_A2ASSDFDFDSS"/>
      <para funid="F00.01.03"/>
    </in:system> 
  </soap:Header>
  <soap:Body>
    <in:business xmlns:in="http://www.molss.gov.cn/">
      <para startrow="1"/>
      <para row_count="-1"/>
      <para pagesize="500"/>
      <para clientsql="( aac002 = &apos;430302195806251012&apos;)"/>
      <para functionid="F27.06"/>
      <paraset name="paralist">
        <row aac003="徐A" rown="1" />
        <row aac003="徐B" rown="2" />
        <row aac003="徐C" rown="3" />
        <row aac003="徐D" rown="4" />
      </paraset>
    </in:business>
  </soap:Body>
 </soap:Envelope>"""
}

class Parameters(
    @transient val funId: String
)

@Namespaces(
  "soap" -> "http://schemas.xmlsoap.org/soap/envelope/"
)
@Node("soap:Envelope")
case class InEnvelope[T](
    @Attribute("soap:encodingStyle")
    var encodingStyle: String,
    @Node("soap:Header")
    var header: InHeader,
    @Node("soap:Body")
    var body: InBody[T]
)

case class System(
    @AttrNode("para", "usr")
    var user: String,
    @AttrNode("para", "pwd")
    var password: String,
    @AttrNode("para", "funid")
    var funId: String
)

case class InHeader(
    @Node("in:system")
    @Namespaces(
      "in" -> "http://www.molss.gov.cn"
    )
    var system: System
)

case class InBody[T](
    @Node("in:business")
    @Namespaces(
      "in" -> "http://www.molss.gov.cn/"
    )
    var business: T
)

case class Business(
    @AttrNode("para", "startrow")
    var startRow: Int,
    @AttrNode("para", "row_count")
    var rowCount: Int,
    @AttrNode("para", "pagesize")
    var pageSize: Int,
    @AttrNode("para", "clientsql")
    var clientSql: String,
    @AttrNode("para", "functionid")
    var functionId: String,
    @Node("paraset")
    var paraSet: ParaSet
)

case class ParaSet(
    @Attribute("name")
    var name: String,
    @Node("row")
    var rowList: List[Row]
)

case class Row(
    @Attribute("aac003")
    var name: String,
    @Attribute("rown")
    var number: Int
)

object XmlTest extends TestSuite {
  override def tests: Tests =
    Tests {
      test("toObject") {
        val inst = Test.xml.toElement.toObject[InEnvelope[Business]]
        println(inst)

        println(inst.toXml(true, """<?xml version="1.0" encoding="GBK"?>"""))
      }
    }
}
