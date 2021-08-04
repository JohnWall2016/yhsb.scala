package yhsb.cjb.net.protocol

/** 回头看居保附表2 */
class LookBackTable1Audit(
    operator: String = "",
    auditState: String = "1",
) extends PageRequest[LookBackTable1Audit.Item]("jmcbckxxShQuery") {
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
  val aae011rh = ""
  val aae005rh = ""
}

object LookBackTable1Audit {
  def apply(
    operator: String = "",
    auditState: String = "1",
  ) = new LookBackTable1Audit(operator, auditState)

  case class Item(

  )
}