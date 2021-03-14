package yhsb.cjb.net.protocol

import yhsb.base.json.Jsonable
import yhsb.base.json.Json.JsonName
import yhsb.cjb.net.Request
import yhsb.base.json.JsonField
import yhsb.cjb.net.PageRequest
import yhsb.base.collection.BiMap
import scala.util.matching.Regex

import java.math.{BigDecimal => JBigDecimal}

case class SysLogin(
    @JsonName("username") userName: String,
    @JsonName("passwd") password: String
) extends Request("syslogin")

case class CbxxQuery(
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

object JBKind
    extends BiMap(
      "011" -> "普通参保人员",
      "021" -> "残一级",
      "022" -> "残二级",
      "023" -> "残三级",
      "031" -> "特困一级",
      "032" -> "特困二级",
      "033" -> "特困三级",
      "051" -> "贫困人口一级",
      "052" -> "贫困人口二级",
      "053" -> "贫困人口三级",
      "061" -> "低保对象一级",
      "062" -> "低保对象二级",
      "063" -> "低保对象三级",
      "071" -> "计生特扶人员",
      "090" -> "其他"
    )

class JBKind extends JsonField {
  override def valueMap = JBKind
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

trait XzqhName {
  val czName: String
  lazy val dwName = Xzqh.getDwName(czName)
  lazy val dwAndCsName = Xzqh.getDwAndCsName(czName)
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
    //@JsonName("aaf101") xzqhCode: String,
    @JsonName("aaf102") czName: String
    //@JsonName("aaf103") csName: String
) extends Jsonable
    with JBState
    with XzqhName {
  
  def valid = idcard != null && !idcard.isEmpty()

  def invalid = !valid

}

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

object Xzqh {
  val codeMap = new BiMap(
    "43030200" -> "代发虚拟乡镇",
    "43030201" -> "长城乡",
    "43030202" -> "昭潭街道",
    "43030203" -> "先锋街道",
    "43030204" -> "万楼街道",
    "43030205" -> "（原）鹤岭镇",
    "43030206" -> "楠竹山镇",
    "43030207" -> "姜畲镇",
    "43030208" -> "鹤岭镇",
    "43030209" -> "城正街街道",
    "43030210" -> "雨湖路街道",
    "43030211" -> "（原）平政路街道",
    "43030212" -> "云塘街道",
    "43030213" -> "窑湾街道",
    "43030214" -> "（原）窑湾街道",
    "43030215" -> "广场街道",
    "43030216" -> "（原）羊牯塘街道"
  )

  val regexps = Seq[Regex](
    "湘潭市雨湖区((.*?乡)(.*?社区)).*".r,
    "湘潭市雨湖区((.*?乡)(.*?村)).*".r,
    "湘潭市雨湖区((.*?乡)(.*?政府机关)).*".r,
    "湘潭市雨湖区((.*?街道)办事处(.*?社区)).*".r,
    "湘潭市雨湖区((.*?街道)办事处(.*?政府机关)).*".r,
    "湘潭市雨湖区((.*?镇)(.*?社区)).*".r,
    "湘潭市雨湖区((.*?镇)(.*?居委会)).*".r,
    "湘潭市雨湖区((.*?镇)(.*?村)).*".r,
    "湘潭市雨湖区((.*?街道)办事处(.*?村)).*".r,
    "湘潭市雨湖区((.*?镇)(.*?政府机关)).*".r,
    "湘潭市雨湖区((.*?街道)办事处(.*))".r
  )

  def getDwName(fullName: String): Option[String] = {
    for (r <- Xzqh.regexps) {
      fullName match {
        case r(_, n, _) => return Some(n)
        case _          =>
      }
    }
    None
  }

  def getDwAndCsName(fullName: String): Option[(String, String)] = {
    for (r <- Xzqh.regexps) {
      fullName match {
        case r(_, dw, cs) => return Some((dw, cs))
        case _            =>
      }
    }
    None
  }
}

case class JfxxQuery(
    @JsonName("aac002") idcard: String
) extends PageRequest(
      "executeSncbqkcxjfxxQ",
      pageSize = 500
    )

object Jfxx {
  class Type extends JsonField {
    override def valueMap = {
      case "10" => "正常应缴"
      case "31" => "补缴"
      case ty => s"未知值: $ty"
    }
  }

  class Item extends JsonField {
    override def valueMap = {
      case "1" => "个人缴费"
      case "3" => "省级财政补贴"
      case "4" => "市级财政补贴"
      case "5" => "县级财政补贴"
      case "11" => "政府代缴"
      case ty => s"未知值: $ty"
    }
  }

  class Method extends JsonField {
    override def valueMap = {
      case "2" => "银行代收"
      case "3" => "经办机构自收"
      case ty => s"未知值: $ty"
    }
  }
}

case class Jfxx(
    @JsonName("aae003") year: Int, // 缴费年度
    @JsonName("aae013") memo: String, // 备注
    @JsonName("aae022") amount: JBigDecimal, // 金额
    @JsonName("aaa115") typ: Jfxx.Type, // 缴费类型
    @JsonName("aae341") item: Jfxx.Item, // 缴费项目
    @JsonName("aab033") method: Jfxx.Method, // 缴费方式
    @JsonName("aae006") payedOffDay: String, // 划拨日期
    @JsonName("aaa027") agency: String, // 社保机构
    @JsonName("aaf101") xzqhCode: String, // 行政区划代码
) extends Jsonable {
  def isPayedOff = payedOffDay != null
}