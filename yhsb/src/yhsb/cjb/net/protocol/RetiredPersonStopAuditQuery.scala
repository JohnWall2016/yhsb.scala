package yhsb
package cjb.net.protocol

/** 待遇人员终止审核查询 */
class RetiredPersonStopAuditQuery(
    idCard: String = "",
    auditState: String = "0",
    operator: String = ""
) extends PageRequest[RetiredPersonStopAuditQuery.Item](
      "dyzzfhPerInfoList",
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
  val aae015 = ""
  val aae015s = ""

  @JsonName("aac002")
  val idCard_ = idCard

  val aac003 = ""
  val aac009 = ""
  val aae160 = ""

  val aic301 = ""
}

object RetiredPersonStopAuditQuery {
  def apply(
      idCard: String = "",
      auditState: String = "0",
      operator: String = ""
  ): RetiredPersonStopAuditQuery =
    new RetiredPersonStopAuditQuery(idCard, auditState, operator)

  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      /** 终止年月 */
      @JsonName("aic301")
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
      /** 多拨扣回额 */
      @JsonName("aae422")
      chargeBackAmount: JBigDecimal,
      /** 抵扣金额 */
      @JsonName("aae424")
      deductAmount: JBigDecimal,
      /** 退款金额 */
      @JsonName("aae425")
      refundAmount: JBigDecimal,
      @JsonName("aae011")
      operator: String,
      /** 经办时间 */
      @JsonName("aae036")
      opTime: String,
      aaz176: Int
  )
}
