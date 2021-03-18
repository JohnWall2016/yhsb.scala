package yhsb
package cjb.net.protocol

/**
 * 代发支付单查询
 */
class DelegatePaymentQuery(
    dfPayType: String,
    yearMonth: String,
    state: String = "0"
) extends PageRequest[DelegatePaymentQuery.Item](
      "dfpayffzfdjQuery"
    ) {

  /** 代发支付类型 */
  @JsonName("aaa121")
  val dfPayType_ = dfPayType

  /** 支付单号 */
  @JsonName("aaz031")
  val payList = ""

  /** 发放年月 */
  @JsonName("aae002")
  val yearMonth_ = yearMonth

  @JsonName("aae089")
  val state_ = state
}

object DelegatePaymentQuery {
  def apply(
    dfPayType: String,
    yearMonth: String,
    state: String = "0"
  ) = new DelegatePaymentQuery(dfPayType, yearMonth, state)

  case class Item(
      /** 业务类型中文名 */
      @JsonName("aaa121")
      typeCh: String,
      /** 付款单号 */
      @JsonName("aaz031")
      payList: Int,
      /** 支付银行编码 */
      @JsonName("bie013")
      bankType: String
  )
}
