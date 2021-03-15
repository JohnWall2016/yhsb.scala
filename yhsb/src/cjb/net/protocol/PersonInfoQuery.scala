package yhsb
package cjb.net.protocol

/** 个人信息综合查询 */
class PersonInfoQuery(
    idCard: String,
    name: String
) extends PageRequest("zhcxgrinfoQuery") {
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

  val aae030str = ""
  val aae030end = ""

  val aae476 = ""
  val aae480 = ""
  val aae479 = ""
  val aac058 = ""

  @JsonName("aac002")
  val idCard_ = idCard

  val aae478 = ""

  @JsonName("aac003")
  val name_ = name

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
      zdName: String
  ) extends JBState
}
