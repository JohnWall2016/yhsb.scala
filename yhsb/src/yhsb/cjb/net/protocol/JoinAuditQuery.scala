package yhsb.cjb.net.protocol

/** 参保审核查询 */
class JoinAuditQuery(
    startDate: String = "",
    endDate: String = "",
    auditState: String = "0",
    operator: String = ""
) extends PageRequest[JoinAuditQuery.Item](
      "cbshQuery",
      pageSize = 500
    ) {
  val aaf013 = ""
  val aaf030 = ""

  @JsonName("aae016")
  val auditState_ = auditState

  @JsonName("aae011")
  val operator_ = operator

  val aae036 = ""
  val aae036s = ""

  val aae014 = ""

  @JsonName("aae015")
  val startDate_ = startDate

  @JsonName("aae015s")
  val endDate_ = endDate

  val aac009 = ""
  val aac002 = ""
  val aac003 = ""
  val sfccb = ""

}

object JoinAuditQuery {
  def apply(
      startDate: String = "",
      endDate: String = "",
      auditState: String = "0",
      operator: String = ""
  ) =
    new JoinAuditQuery(
      startDate,
      endDate,
      auditState,
      operator
    )
  
  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      @JsonName("aac006")
      birthDay: String,
      @JsonName("aaf102")
      czName: String,
      @JsonName("aae011")
      operator: String,
      /** 经办时间 */
      @JsonName("aae036")
      opTime: String
  ) extends DivisionName
}
