package yhsb
package cjb.net.protocol

/** 待遇人员暂停审核查询 */
class RetiredPersonPauseAuditQuery(
    idCard: String = "",
    auditState: String = "0"
) extends PageRequest(
      "queryAllPausePersonInfosForAuditService",
      pageSize = 500
    ) {
  val aaf013 = ""
  val aaz070 = ""

  @JsonName("aac002")
  val idCard_ = idCard

  /** 起始暂停年月 */
  val aae141 = ""

  /** 截止暂停年月 */
  val aae141s = ""

  /** 审核状态 */
  @JsonName("aae016")
  val auditState_ = auditState

  /** 起始经办时间 */
  val aae036 = ""

  /** 截止经办时间 */
  val aae036s = ""

  /** 户籍性质 */
  val aac009 = ""

  /** 起始审核时间 */
  val aae015 = ""

  /** 截止审核时间 */
  val aae015s = ""

  /** 待遇状态 */
  val aae116 = ""

  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      /** 暂停年月 */
      @JsonName("aae141")
      pauseYearMonth: Int,
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
      aaz173: Int
  ) extends DivisionName
}
