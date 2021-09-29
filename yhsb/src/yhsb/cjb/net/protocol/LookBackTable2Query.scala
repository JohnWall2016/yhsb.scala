package yhsb.cjb.net.protocol

import scala.collection.immutable.SeqMap

/** 回头看居保附表2查询 */
class LookBackTable2Query(
    xzqhCode: String = "",
    auditState: String = "0" // 0 - 已核查 1 - 未核查
) extends PageRequest[LookBackTable2Query.Item]("htkhcjgcxQuery") {
  @JsonName("aaf013")
  val xzqhCode_ = xzqhCode

  val aaf030 = ""
  val aac002 = ""
  val aac003 = ""

  @JsonName("aae016")
  val state = auditState

  val aae618 = ""
  val aae621 = ""

}

object LookBackTable2Query {
  def apply(
      xzqhCode: String = "",
      auditState: String = "0" // 0 - 已核查 1 - 未核查
  ) = new LookBackTable2Query(xzqhCode, auditState)

  case class Item(
  )

  val columnMap = SeqMap(
    "aaf102" -> "村组名称",
    "aac002" -> "身份证号码",
    "aac003" -> "姓名",
    "aae016" -> "是否核查",
    "aae010" -> "银行账号",
    "bie013" -> "银行名称",
    "aae617" -> "社保卡标识",
    "aae116" -> "待遇状态",
    "aae031" -> "待遇终止或暂停年月",
    "aae005" -> "联系方式",
    "aae618" -> "是否健在",
    "aae619" -> "是否本人（亲属）领卡在手",
    "swrq" -> "死亡日期",
    "aae620" -> "社保卡是否亲属持有",
    "aae621" -> "异常情况",
    "bz" -> "核查结果备注",
    "aae622" -> "核查方式",
    "aae005hc" -> "参保人联系方式",
    "aae011rh" -> "入户核查人姓名",
    "aae005rh" -> "入户核查人联系方式"
  )
}
