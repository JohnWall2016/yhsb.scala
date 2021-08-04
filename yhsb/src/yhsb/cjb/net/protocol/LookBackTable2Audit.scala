package yhsb.cjb.net.protocol

/** 回头看居保附表2 */
class LookBackTable2Audit(
    operator: String = "",
    auditState: String = "1",
) extends PageRequest[LookBackTable1Audit.Item]("hcjsShQuery") {
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
  val aac003 = ""
}

object LookBackTable2Audit {
  def apply(
    operator: String = "",
    auditState: String = "1",
  ) = new LookBackTable2Audit(operator, auditState)

  case class Item(

  )
}