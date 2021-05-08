package yhsb
package cjb.net.protocol

import scala.collection.SeqMap

/** 个人信息综合查询 */
class PersonInfoQuery(
    idCard: String = "",
    name: String = "",
    joinedStartDate: String = "", // "yyyy-MM-dd"
    joinedEndDate: String = "",
) extends PageRequest[PersonInfoQuery.Item]("zhcxgrinfoQuery") {
  val aaf013 = ""
  val aaz070 = ""
  val aaf101 = ""
  val aac009 = ""

  /** 参保状态: "1"-正常参保 "2"-暂停参保 "4"-终止参保 "0"-未参保 */
  @JsonName("aac008")
  val cbState = ""

  /** 缴费状态: "1"-参保缴费 "2"-暂停缴费 "3"-终止缴费 */
  @JsonName("aac031")
  val jfState = ""

  val aac006str = ""
  val aac006end = ""

  val aac066 = ""

  val aae030str = joinedStartDate
  val aae030end = joinedEndDate

  val aae476 = ""
  val aae480 = ""
  val aae479 = ""
  val aac058 = ""

  @JsonName("aac002")
  val idCard_ = idCard

  val aae478 = ""

  @JsonName("aac003")
  val name_ = name
}

object PersonInfoQuery {
  def apply(
    joinedStartDate: String, // "yyyy-MM-dd"
    joinedEndDate: String,
  ) = new PersonInfoQuery(
    joinedStartDate = joinedStartDate,
    joinedEndDate = joinedEndDate,
  )

  def apply(
    idCard: String
  ) = new PersonInfoQuery(idCard)

  case class Item(
      @JsonName("aac001")
      pid: Int,
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      @JsonName("aac006")
      birthDay: String,
      @JsonName("aac008")
      cbState: CBState,
      /** 户口所在地 */
      @JsonName("aac010")
      hkArea: String,
      @JsonName("aac031")
      jfState: JFState,
      @JsonName("aae005")
      phoneNumber: String,
      @JsonName("aae006")
      address: String,
      @JsonName("aae010")
      bankCardNumber: String,
      /** 乡镇街区划编码 */
      @JsonName("aaf101")
      xzName: String,
      /** 村社名称区划编码 */
      @JsonName("aaf102")
      csName: String,
      /** 组队名称区划编码 */
      @JsonName("aaf103")
      zdName: String,
      aaz159: Int,
  ) extends JBState

  val columnMap = SeqMap(
    "aaf102" -> "行政区划",
    "aac009" -> "户籍性质",
    "aac001" -> "个人编号",
    "aac003" -> "姓名",
    "aaa103" -> "证件类型",
    "aac002" -> "证件号码",
    "aac004" -> "性别",
    "aac006" -> "出生日期",
    "aac005" -> "民族",
    "aac066" -> "参保身份",
    "aae016" -> "业务状态",
    "aac008" -> "参保状态",
    "aac031" -> "缴费状态",
    "aae476" -> "被征地身份",
    "aae480" -> "退捕渔民身份",
    "aae479" -> "核工业身份",
    "aae478" -> "老农保身份",
    "aac049" -> "参保时间",
  )
}
