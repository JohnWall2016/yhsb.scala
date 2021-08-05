package yhsb.cjb.net.protocol

import yhsb.base.struct.NotNull

/** 回头看居保附表2 */
class LookBackTable2Audit(
    operator: String = "",
    idCard: String = "",
    auditState: String = "1"
) extends PageRequest[LookBackTable2Audit.Item]("hcjsShQuery") {
  val aaf013 = ""
  val aaf030 = ""

  @JsonName("aae011")
  val operator_ = operator

  val aae036 = ""
  val aae036s = ""
  val aae014 = ""
  val aae015 = ""
  val aae015s = ""

  @JsonName("aae016")
  val state = auditState

  val aac002 = ""
  val aac003 = idCard
}

object LookBackTable2Audit {
  def apply(
      operator: String = "",
      idCard: String = "",
      auditState: String = "1"
  ) = new LookBackTable2Audit(operator, idCard, auditState)

  case class Item(
      aac003: String,
      aae622: String,
      aae621: String,
      swrq: String,
      aae618: String,
      aae038: String,
      aae016: String,
      aae005hc: String,
      aae036: String,
      aae015: String,
      aae620: String,
      aae011rh: String,
      aae005rh: String,
      aae011: String,
      aaf102: String,
      aac002: String
  ) extends NotNull
}
