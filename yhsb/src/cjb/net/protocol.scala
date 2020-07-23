package yhsb.cjb.net.protocol

import yhsb.util.json.Jsonable
import yhsb.util.json.Json.JsonName
import yhsb.cjb.net.Request
import yhsb.util.json.JsonField
import yhsb.cjb.net.PageRequest

case class SysLogin(
    @JsonName("username") userName: String,
    @JsonName("passwd") password: String
) extends Request("syslogin")

case class CbxxRequest(
    @JsonName("aac002") idcard: String
) extends Request("executeSncbxxConQ")

class CBState extends JsonField {
  override def valueMap = {
    case "0" => "未参保"
    case "1" => "正常参保"
    case "2" => "暂停参保"
    case "4" => "终止参保"
  }
}

class JFState extends JsonField {
  override def valueMap = {
    case "1" => "参保缴费"
    case "2" => "暂停缴费"
    case "3" => "终止缴费"
  }
}

class JBKind extends JsonField {
  override def valueMap = {
    case "011" => "普通参保人员"
    case "021" => "残一级"
    case "022" => "残二级"
    case "031" => "特困一级"
    case "051" => "贫困人口一级"
    case "061" => "低保对象一级"
    case "062" => "低保对象二级"
  }
}

trait JBState {
  val cbState: CBState
  val jfState: JFState

  def jbState = {
    if (jfState == null || jfState.value == "0") {
      "未参保"
    } else {
      (jfState.value, cbState.value) match {
        case ("1", "1") => "正常缴费人员"
        case ("1", cb)  => s"未知类型参保缴费人员: $cb"
        case ("2", "2") => "暂停缴费人员"
        case ("2", cb)  => s"未知类型暂停缴费人员: $cb"
        case ("3", "1") => "正常待遇人员"
        case ("3", "2") => "暂停待遇人员"
        case ("3", "4") => "终止参保人员"
        case ("3", cb)  => s"未知类型终止缴费人员: $cb"
        case (jf, cb)   => s"未知类型人员: $jf, $cb"
      }
    }
  }
}

case class Cbxx(
    @JsonName("aac001") pid: Int,
    @JsonName("aac002") idcard: String,
    @JsonName("aac003") name: String,
    @JsonName("aac006") birthDay: String,
    @JsonName("aac008") cbState: CBState,
    @JsonName("aac031") jfState: JFState,
    @JsonName("aac049") cbTime: Int,
    @JsonName("aac066") jbKind: JBKind,
    @JsonName("aaa129") agency: String,
    @JsonName("aae036") opTime: String,
    @JsonName("aaf101") xzqhCode: String,
    @JsonName("aaf102") czName: String,
    @JsonName("aaf103") csName: String
) extends Jsonable
    with JBState

class CbshQuery(
    startDate: String, // "2019-04-29"
    endDate: String,
    shState: String // "1"
) extends PageRequest(
      "cbshQuery",
      pageSize = 500
    ) {
  val aaf013: String = ""
  val aaf030: String = ""
  val aae011: String = ""
  val aae036: String = ""
  val aae036s: String = ""
  val aae014: String = ""
  val aac009: String = ""
  val aac002: String = ""
  val aac003: String = ""
  val sfccb: String = ""

  val aae015 = startDate
  val aae015s = endDate
  val aae016 = shState
}

object CbshQuery {
  def apply(
      startDate: String, // "2019-04-29"
      endDate: String = "",
      shState: String = "1" // "1"
  ) = new CbshQuery(startDate, endDate, shState)
}

case class Cbsh(
    @JsonName("aac002") idcard: String,
    @JsonName("aac003") name: String,
    @JsonName("aac006") birthDay: String
) extends Jsonable
