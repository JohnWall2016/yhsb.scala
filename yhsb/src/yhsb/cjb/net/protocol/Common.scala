package yhsb.cjb.net.protocol

import scala.util.matching.Regex
import scala.collection.mutable

import yhsb.base.struct.MapField
import yhsb.base.collection.BiMap
import yhsb.base.util.UtilOps
import yhsb.cjb.net

class CBState extends MapField {
  override def valueMap = {
    case "0" => "未参保"
    case "1" => "正常参保"
    case "2" => "暂停参保"
    case "4" => "终止参保"
  }
}

class JFState extends MapField {
  override def valueMap = {
    case "1" => "参保缴费"
    case "2" => "暂停缴费"
    case "3" => "终止缴费"
  }
}

class JFType extends MapField {
  override def valueMap = {
    case "10" => "正常应缴"
    case "31" => "补缴"
  }
}

class JFItem extends MapField {
  override def valueMap = {
    case "1"  => "个人缴费"
    case "3"  => "省级财政补贴"
    case "4"  => "市级财政补贴"
    case "5"  => "县级财政补贴"
    case "11" => "政府代缴"
  }
}

class JFMethod extends MapField {
  override def valueMap = {
    case "2" => "银行代收"
    case "3" => "经办机构自收"
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
  ) {
    lazy val special = subBiMap(
      Set("021", "022", "031", "051", "061", "062")
    )
  }

class JBKind extends MapField {
  override def valueMap = JBKind
}

trait JBState {
  val cbState: CBState
  val jfState: JFState

  def jbState = {
    if (jfState == null || jfState.value == "0") {
      "未参保"
    } else {
      JBState.jbStateMap.getOrElse(
        (cbState.value, jfState.value),
        s"未知类型: ${cbState.value}, ${jfState.value}"
      )
    }
  }
}

object JBState {
  val jbStateMap = Map(
    ("1", "1") -> "正常缴费",
    ("2", "2") -> "暂停缴费",
    ("1", "3") -> "正常待遇",
    ("2", "3") -> "暂停待遇",
    ("4", "3") -> "终止参保"
  )
}

trait IdCardValid {
  val idCard: String

  def valid = idCard != null && idCard.nonEmpty

  def invalid = !valid
}

trait DivisionName {
  val czName: String
  lazy val dwName = Division.getDwName(czName)
  lazy val csName = Division.getCsName(czName)
  lazy val dwAndCsName = Division.getDwAndCsName(czName)
}

object Division {
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

  val regExps = Seq[Regex](
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
    for (r <- Division.regExps) {
      fullName match {
        case r(_, n, _) => return Some(n)
        case _          =>
      }
    }
    None
  }

  def getCsName(fullName: String): Option[String] = {
    for (r <- Division.regExps) {
      fullName match {
        case r(_, _, n) => return Some(n)
        case _          =>
      }
    }
    None
  }

  def getDwAndCsName(fullName: String): Option[(String, String)] = {
    for (r <- Division.regExps) {
      fullName match {
        case r(_, dw, cs) => return Some((dw, cs))
        case _            =>
      }
    }
    None
  }

  implicit class GroupOps[T](iter: Iterable[(String, T)]) {
    def groupByDwName(): collection.Map[String, Iterable[T]] = {
      val map = mutable.LinkedHashMap[String, mutable.ListBuffer[T]]()
      iter.foreach { case (division, data) =>
        val dw = getDwName(division) match {
          case Some(value) => value
          case None        => throw new Exception(s"未匹配行政区划: $division")
        }
        val list = map.getOrElseUpdate(dw, mutable.ListBuffer())
        list.addOne(data)
      }
      map
    }

    def groupByDwAndCsName()
        : collection.Map[String, collection.Map[String, Iterable[T]]] = {
      val map = mutable
        .LinkedHashMap[String, mutable.Map[String, mutable.ListBuffer[T]]]()
      iter.foreach { case (division, data) =>
        val (dw, cs) = getDwAndCsName(division) match {
          case Some(value) => value
          case None        => throw new Exception(s"未匹配行政区划: '$division'")
        }
        val subMap = map.getOrElseUpdate(dw, mutable.LinkedHashMap())
        val list = subMap.getOrElseUpdate(cs, mutable.ListBuffer())
        list.addOne(data)
      }
      map
    }
  }
}

class BankType extends MapField {
  override def valueMap = {
    case "LY" => "中国农业银行"
    case "ZG" => "中国银行"
    case "JS" => "中国建设银行"
    case "NH" => "农村信用合作社"
    case "YZ" => "邮政"
    case "JT" => "交通银行"
    case "GS" => "中国工商银行"
  }
}

class DFType extends MapField {
  override def valueMap = {
    case "801" => "独生子女"
    case "802" => "乡村教师"
    case "803" => "乡村医生"
    case "807" => "电影放映"
    case "808" => "核工业"
  }
}

object DFType {
  def apply(dfType: String) =
    (new DFType).set { _.value = dfType }
}

class DFPayType extends MapField {
  override def valueMap = {
    case "DF0001" => "独生子女"
    case "DF0002" => "乡村教师"
    case "DF0003" => "乡村医生"
    case "DF0007" => "电影放映员"
    case "DF0008" => "核工业"
  }
}

object DFPayType {
  def apply(dfPayType: String) =
    (new DFPayType).set { _.value = dfPayType }
}

class DFState extends MapField {
  override def valueMap = {
    case "1" => "正常发放"
    case "2" => "暂停发放"
    case "3" => "终止发放"
  }
}

/** 暂停参保原因 */
class PauseReason extends MapField {
  override def valueMap = {
    case "1201" => "养老待遇享受人员未提供生存证明"
    case "1299" => "其他原因暂停养老待遇"
    case "6399" => "其他原因中断缴费"
  }
}

/** 审核状态 */
class AuditState extends MapField {
  override def valueMap = {
    case "0" => "未审核"
    case "1" => "审核通过"
  }
}

/** 终止参保原因 */
class StopReason extends MapField {
  override def valueMap = {
    case "1401" => "死亡"
    case "1406" => "出外定居"
    case "1407" => "参加职保"
    case "1499" => "其他原因"
    case "6401" => "死亡"
    case "6406" => "出外定居"
    case "6407" => "参加职保"
    case "6499" => "其他原因"
  }
}

/** 支付业务类型 */
class PayType extends MapField {
  override def valueMap = {
    case "F10004" => "重复缴费退费"
    case "F10006" => "享受终止退保"
    case "F10007" => "缴费调整退款"
  }
}

object SessionOps {
  implicit class CeaseInfo(session: net.Session) {
    object CeaseType extends Enumeration {
      type CeaseType = Value

      val PayingPause = Value("缴费暂停")
      val RetirePause = Value("待遇暂停")
      val PayingStop = Value("缴费终止")
      val RetiredStop = Value("待遇终止")
    }

    import CeaseType._

    case class CeaseInfo(
        ceaseType: CeaseType,
        reason: String,
        yearMonth: String,
        auditState: AuditState,
        memo: String,
        auditDate: Option[String],
        bankName: Option[String] = None
    )
    def getPauseInfoByIdCard(idCard: String): Option[CeaseInfo] = {
      val rpResult = session
        .request(RetiredPersonPauseAuditQuery(idCard, "1"))
      if (rpResult.nonEmpty) {
        val it = rpResult.maxBy(_.auditDate)
        return Some(
          CeaseInfo(
            RetirePause,
            it.reason.toString,
            it.pauseYearMonth.toString,
            it.auditState,
            it.memo,
            Option(it.auditDate)
          )
        )
      } else {
        val ppResult = session
          .request(PayingPersonPauseAuditQuery(idCard, "1"))
        if (ppResult.nonEmpty) {
          val it = ppResult.maxBy(_.auditDate)
          return Some(
            CeaseInfo(
              PayingPause,
              it.reason.toString,
              it.pauseYearMonth,
              it.auditState,
              it.memo,
              Option(it.auditDate)
            )
          )
        }
      }
      None
    }

    def getStopInfoByIdCard(
        idCard: String,
        additionalInfo: Boolean = false
    ): Option[CeaseInfo] = {
      val rpResult = session
        .request(RetiredPersonStopAuditQuery(idCard, "1"))
      if (rpResult.nonEmpty) {
        val it = rpResult.head
        val bankName =
          if (additionalInfo) {
            session
              .request(RetiredPersonStopAuditDetailQuery(it))
              .headOption match {
              case Some(item) => Option(item.bankType.name)
              case _          => None
            }
          } else {
            None
          }
        return Some(
          CeaseInfo(
            RetirePause,
            it.reason.toString,
            it.stopYearMonth.toString,
            it.auditState,
            it.memo,
            Option(it.auditDate),
            bankName
          )
        )
      } else {
        val ppResult = session
          .request(PayingPersonStopAuditQuery(idCard, "1"))
        if (ppResult.nonEmpty) {
          val it = ppResult.head
          val bankName =
            if (additionalInfo) {
              session
                .request(PayingPersonStopAuditDetailQuery(it))
                .headOption match {
                case Some(item) => Option(item.bankType.name)
                case _          => None
              }
            } else {
              None
            }
          return Some(
            CeaseInfo(
              PayingStop,
              it.reason.toString,
              it.stopYearMonth.toString,
              it.auditState,
              it.memo,
              Option(it.auditDate),
              bankName
            )
          )
        }
      }
      None
    }
  }
}
