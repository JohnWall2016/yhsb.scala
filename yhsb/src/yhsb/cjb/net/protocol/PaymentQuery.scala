package yhsb.cjb.net.protocol

class PaymentQuery(
    startYearMonth: String,
    endYearMonth: String,
    payState: PayState = PayState.Sucess
) extends PageRequest[PaymentQuery.Item](
    "executePayffqkcxQuery",
    1,
    10,
    sortOptions = Map(
      "dataKey" -> "aaf103",
      "sortDirection" -> "ascending"
    )
  ) {
  val aaf013 = ""
  val aaf030 = ""

  @JsonName("aac002")
  val idCard = ""

  @JsonName("aae002str")
  val startYearMonth_ = startYearMonth

  @JsonName("aae002end")
  val endYearMonth_ = endYearMonth

  val aac009 = ""
  val aac066 = ""

  @JsonName("aae117")
  val payState_ = payState
}

object PaymentQuery {
  def apply(
      startYearMonth: String,
      endYearMonth: String = null,
      payState: PayState = PayState.Sucess
  ) = new PaymentQuery(
    startYearMonth,
    if (endYearMonth == null) startYearMonth else endYearMonth,
    payState
  )

  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      @JsonName("aac004")
      sex: String,
      /** 组队名称区划编码 */
      @JsonName("aaf103")
      zdName: String,
      @JsonName("aae019")
      amount: BigDecimal,
  )
}
