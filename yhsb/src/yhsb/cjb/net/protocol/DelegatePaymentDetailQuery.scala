package yhsb
package cjb.net.protocol

/**
  * 代发支付单明细查询
  */
class DelegatePaymentDetailQuery(
    payList: Int,
    page: Int = 1,
    pageSize: Int = 500
) extends PageRequest[DelegatePaymentDetailQuery.Item](
      "dfpayffzfdjmxQuery",
      page,
      pageSize
    ) {

  /** 支付单号 */
  @JsonName("aaz031")
  val payList_ = s"$payList"
}

object DelegatePaymentDetailQuery {
  def apply(
    payList: Int,
    page: Int = 1,
    pageSize: Int = 500
  ) = new DelegatePaymentDetailQuery(payList, page, pageSize)

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
      /** 支付标志 */
      @JsonName("aae117")
      flag: String,
      /** 发放年月 */
      @JsonName("aae002")
      yearMonth: Int,
      /** 付款单号 */
      @JsonName("aaz031")
      payList: Int,
      /** 个人单号 */
      @JsonName("aaz220")
      personalPayList: Long,
      /** 支付总金额 */
      @JsonName("aae019")
      amount: JBigDecimal
  )
}
