package yhsb
package cjb.net.protocol

/** 缴费人员终止审核查询 */
class WorkingPersonStopAuditQuery(
    idCard: String = "",
    auditState: String = "0",
    operator: String = "",
    startAuditDate: String = "",
    endAuditDate: String = "",
    recieveType: String = "1"
) extends PageRequest[WorkingPersonStopAuditQuery.Item](
    "cbzzfhPerInfoList",
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
  val startAuditDate_ = startAuditDate

  @JsonName("aae015s")
  val endAuditDate_ = endAuditDate

  @JsonName("aac002")
  val idCard_ = idCard

  val aac003 = ""
  val aac009 = ""
  val aae160 = ""

  val aae465 = recieveType
}

object WorkingPersonStopAuditQuery {
  def apply(
      idCard: String = "",
      auditState: String = "0",
      operator: String = "",
      startAuditDate: String = "",
      endAuditDate: String = "",
      recieveType: String = "1"
  ): WorkingPersonStopAuditQuery =
    new WorkingPersonStopAuditQuery(
      idCard,
      auditState,
      operator,
      startAuditDate,
      endAuditDate,
      recieveType
    )

  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      /** 终止年月 */
      @JsonName("aae031")
      stopYearMonth: Int,
      @JsonName("aae160")
      reason: StopReason,
      /** 审核日期 */
      @JsonName("aae015")
      auditDate: String,
      @JsonName("aae016")
      auditState: AuditState,
      /** 备注 */
      @JsonName("aae013")
      memo: String,
      /** 银行户名 */
      @JsonName("aae009")
      bankName: String,
      /** 银行账号 */
      @JsonName("aae010")
      bankAccount: String,
      /** 退款金额 */
      @JsonName("aae025")
      refundAmount: JBigDecimal,
      @JsonName("aae011")
      operator: String,
      /** 经办时间 */
      @JsonName("aae036")
      opTime: String,
      aaz038: Int,
      aac001: Int
  )
}
