package yhsb
package cjb.net.protocol

/** 缴费人员暂停审核查询 */
class WorkingPersonPauseAuditQuery(
    idCard: String = "",
    auditState: String = "0",
    startAuditDate: String = "",
    endAuditDate: String = "",
) extends PageRequest[WorkingPersonPauseAuditQuery.Item](
      "queryJfZtPersonInfosForAuditService",
      pageSize = 500
    ) {
  val aaf013 = ""
  val aaz070 = ""

  @JsonName("aac002")
  val idCard_ = idCard

  /** 起始经办时间 */
  val aae036 = ""

  /** 截止经办时间 */
  val aae036s = ""

  /** 审核状态 */
  @JsonName("aae016")
  val auditState_ = auditState

  /** 起始审核时间 */
  @JsonName("aae015")
  val startAuditDate_ = startAuditDate

  /** 截止审核时间 */
  @JsonName("aae015s")
  val endAuditDate_ = endAuditDate

  /** 户籍性质 */
  val aac009 = ""
}

object WorkingPersonPauseAuditQuery {
  def apply(
    idCard: String = "",
    auditState: String = "0",
    startAuditDate: String = "",
    endAuditDate: String = "",
  ): WorkingPersonPauseAuditQuery =
    new WorkingPersonPauseAuditQuery(idCard, auditState, startAuditDate, endAuditDate)

  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      /** 暂停年月 */
      @JsonName("aae035")
      pauseYearMonth: String,
      /** 暂停原因? */
      @JsonName("aae160")
      reason: PauseReason,
      /** 经办时间 */
      @JsonName("aae036")
      opTime: String,
      /** 审核日期 */
      @JsonName("aae015")
      auditDate: String,
      @JsonName("aae016")
      auditState: AuditState,
      /** 村组名称 */
      @JsonName("aaf102")
      czName: String,
      @JsonName("aae013")
      memo: String,
      @JsonName("aaz163")
      id: Int
  ) extends DivisionName
}