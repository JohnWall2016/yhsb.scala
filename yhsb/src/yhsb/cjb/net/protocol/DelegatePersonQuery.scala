package yhsb
package cjb.net.protocol

/**
 * 代发人员名单查询
 */
class DelegatePersonQuery(
    dfType: String,
    /** 代发参保状态: 1 - 有效, 0 - 无效 */
    cbState: String,
    /** 代发发放状态: 1 - 正常发放, 2 - 暂停发放, 3 - 终止发放 */
    dfState: String,
    page: Int = 1,
    pageSize: Int = 1000
) extends PageRequest[DelegatePersonQuery.Item](
      "executeDfrymdQuery",
      page,
      pageSize,
      sortOptions = Map(
        "dataKey" -> "aaf103",
        "sortDirection" -> "ascending"
      )
    ) {
  val aaf013 = ""
  val aaf030 = ""

  @JsonName("aae100")
  val cbState_ = cbState

  val aac002 = ""
  val aac003 = ""

  @JsonName("aae116")
  val state = dfState

  val aac082 = ""

  @JsonName("aac066")
  val dfType_ = dfType

}

object DelegatePersonQuery {
  def apply(
    dfType: String,
    /** 代发参保状态: 1 - 有效, 0 - 无效 */
    cbState: String,
    /** 代发发放状态: 1 - 正常发放, 2 - 暂停发放, 3 - 终止发放 */
    dfState: String,
    page: Int = 1,
    pageSize: Int = 1000
  ) = new DelegatePersonQuery(dfType, cbState, dfState, page, pageSize)

  case class Item(
      /** 个人编号 */
      @JsonName("aac001")
      pid: Int,
      /** 身份证号码 */
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      /** 村社区名称 */
      @JsonName("aaf103")
      csName: String,
      /** 代发开始年月 */
      @JsonName("aic160")
      startYearMonth: Int,
      /** 代发标准 */
      @JsonName("aae019")
      standard: JBigDecimal,
      /** 代发类型 */
      @JsonName("aac066s")
      dfType: String,
      /** 代发状态 */
      @JsonName("aae116")
      dfState: DFState,
      /** 居保状态 */
      @JsonName("aac008s")
      cbState: CBState,
      /** 代发截至成功发放年月 */
      @JsonName("aae002jz")
      endYearMonth: Int,
      /** 代发截至成功发放金额 */
      @JsonName("aae019jz")
      totalPayed: BigDecimal
  ) extends IdCardValid
}
