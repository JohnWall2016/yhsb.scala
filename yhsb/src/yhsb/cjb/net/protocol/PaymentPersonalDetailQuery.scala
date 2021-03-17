package yhsb
package cjb.net.protocol

import scala.jdk.CollectionConverters._

/** 财务支付管理查询_支付单人员明细 */
class PaymentPersonalDetailQuery(
    payList: String = "",
    yearMonth: String = "",
    state: String = "",
    payType: String = ""
) extends PageRequest[PaymentPersonalDetailQuery.Item](
      "cwzfgl_zfdryQuery",
      1,
      1000,
      totalOpts = Map(
        "dataKey" -> "aae019",
        "aggregate" -> "sum"
      ).asJava
    ) {

  val aaf015 = ""

  @JsonName("aac002")
  val idCard = ""

  @JsonName("aac003")
  val name = ""

  /**
   * 支付单号
   */
  @JsonName("aaz031")
  val payList_ = payList

  /**
   * 支付状态
   */
  @JsonName("aae088")
  val state_ = state

  /**
   * 业务类型: "F10004" - 重复缴费退费; "F10007" - 缴费调整退款;
   * "F10006" - 享受终止退保
   */
  @JsonName("aaa121")
  val payType_ = payType

  /**
   * 发放年月
   */
  @JsonName("aae002")
  val yearMonth_ = yearMonth
}

object PaymentPersonalDetailQuery {
  def apply(item: PaymentQuery.Item) =
    new PaymentPersonalDetailQuery(
      item.payList.toString,
      item.yearMonth.toString,
      item.state,
      item.payType.value
    )

  case class Item(
      /** 身份证号码 */
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      /** 付款单号 */
      @JsonName("aaz031")
      payList: Int,
      /** 支付总金额 */
      @JsonName("aae019")
      amount: JBigDecimal,
      @JsonName("aaa121")
      payType: PayType,
      @JsonName("aaf103")
      csName: String
  )
}
