package yhsb.cjb.net.protocol

import scala.util.matching.Regex
import scala.collection.mutable

import yhsb.base.struct.MapField
import yhsb.base.collection.BiMap

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
    case ty   => s"未知值: $ty"
  }
}

class JFItem extends MapField {
  override def valueMap = {
    case "1"  => "个人缴费"
    case "3"  => "省级财政补贴"
    case "4"  => "市级财政补贴"
    case "5"  => "县级财政补贴"
    case "11" => "政府代缴"
    case ty   => s"未知值: $ty"
  }
}

class JFMethod extends MapField {
  override def valueMap = {
    case "2" => "银行代收"
    case "3" => "经办机构自收"
    case ty  => s"未知值: $ty"
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

  implicit class GroupOps[T](iter: Iterator[(String, T)]) {
    def groupByDwName(): collection.Map[String, Iterable[T]] = {
      val map = mutable.LinkedHashMap[String, mutable.ListBuffer[T]]()
      iter.foreach {
        case (division, data) =>
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
      iter.foreach {
        case (division, data) =>
          val (dw, cs) = getDwAndCsName(division) match {
            case Some(value) => value
            case None        => throw new Exception(s"未匹配行政区划: $division")
          }
          val subMap = map.getOrElseUpdate(dw, mutable.LinkedHashMap())
          val list = subMap.getOrElseUpdate(cs, mutable.ListBuffer())
          list.addOne(data)
      }
      map
    }
  }
}
