package yhsb
package cjb.net.protocol

/**
  * 代发支付单个人明细查询
  */
class DelegatePaymentPersonalDetailQuery(
    pid: Int,
    payList: Int,
    personalPayList: Long,
    page: Int = 1,
    pageSize: Int = 500
) extends PageRequest(
      "dfpayffzfdjgrmxQuery",
      page,
      pageSize
    ) {
  def this(item: DelegatePaymentDetailQuery#Item) =
    this(
      item.pid,
      item.payList,
      item.personalPayList
    )

  /** 个人编号 */
  @JsonName("aac001")
  val pid_ = s"$pid"

  /** 支付单号 */
  @JsonName("aaz031")
  val payList_ = s"$payList"

  /** 支付单号 */
  @JsonName("aaz220")
  val personalPayList_ = s"$personalPayList"

  case class Item(
      /** 待遇日期 */
      @JsonName("aae003")
      date: Int,
      /** 支付标志 */
      @JsonName("aae117")
      flag: String,
      /** 发放年月 */
      @JsonName("aae002")
      yearMonth: String,
      /** 付款单号 */
      @JsonName("aaz031")
      payList: Int,
      /** 支付总金额 */
      @JsonName("aae019")
      amount: JBigDecimal
  )
}
