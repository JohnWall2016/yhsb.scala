package yhsb.cjb.net.protocol

import scala.collection.immutable.SeqMap

/** 回头看居保附表1查询 */
class LookBackTable1Query(
    xzqhCode: String = "",
    auditState: String = "1",
) extends PageRequest[LookBackTable1Query.Item]("jmcbckxxjgQuery") {
  @JsonName("aaf013")
  val xzqhCode_ = xzqhCode

  val aae011 = ""
  val aae036 = ""
  val aae036s = ""
  val aae014 = ""
  val aae015 = ""
  val aae015s = ""
  
  @JsonName("aae016")
  val state = auditState

  val aac002 = ""
  val aac003 = ""
  val aae011rh = ""
  val aae005rh = ""
}

object LookBackTable1Query {
  def apply(
    xzqhCode: String = "",
    auditState: String = "1",
  ) = new LookBackTable1Query(xzqhCode, auditState)

  case class Item(
  )

  val columnMap = SeqMap(
    "aab301" -> "行政区划",
    "aaf102" -> "户籍地址",
    "aac003" -> "姓名",
    "aac002" -> "证件号码",
    "aac008" -> "是否参保",
    "aae140" -> "参保险种",
    "aae030" -> "参保年月",
    "aae619" -> "是否本人（亲属）持卡",
    "aae617" -> "持卡类型",
    "aae010" -> "银行账号",
    "bie013" -> "卡面银行",
    "lkrq" -> "领卡时间",
    "aae622" -> "核查方式",
    "aae005hc" -> "参保人联系方式",
    "aae011rh" -> "入户核查人姓名",
    "aae005rh" -> "入户核查人联系方式",
    "aae011" -> "经办人",
    "aae036" -> "经办时间",
    "aae015" -> "复核人",
    "aae038" -> "复核时间",
    "aae016" -> "审核状态",
    "aae013" -> "备注",
  )
}