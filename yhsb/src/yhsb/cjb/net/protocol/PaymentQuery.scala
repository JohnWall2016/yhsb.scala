package yhsb
package cjb.net.protocol

import scala.jdk.CollectionConverters._

/** 财务支付管理查询 */
class PaymentQuery(
    yearMonth: String,
    state: String = "0"
) extends PageRequest(
      "cwzfglQuery",
      1,
      100,
      totalOpts = Map(
        "dataKey" -> "aae169",
        "aggregate" -> "sum"
      ).asJava
    ) {

  /** 支付类型 */
  @JsonName("aaa121")
  val payType = ""

  /** 支付单号 */
  @JsonName("aaz031")
  val payList = ""

  /** 发放年月 */
  @JsonName("aae002")
  val yearMonth_ = yearMonth

  @JsonName("aae089")
  val state_ = state

  val bie013 = ""

  case class Item(
      /** 支付对象类型: "1" - 月度银行代发, "3" - 个人支付 */
      @JsonName("aaa079")
      objectType: String,
      /** 支付单号 */
      @JsonName("aaz031")
      payList: Int,
      /** 支付状态 */
      @JsonName("aae088")
      state: String,
      @JsonName("aaa121")
      payType: PayType,
      /** 发放年月 */
      @JsonName("aae002")
      yearMonth: Int,
      /** 支付对象银行户名 */
      @JsonName("aae009")
      name: String,
      /** 支付银行编码 */
      @JsonName("bie013")
      bankType: String,
      /** 支付对象银行账号 */
      @JsonName("aae010")
      account: String
  )
}
