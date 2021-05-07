package yhsb.cjb.net.protocol

import scala.collection.SeqMap

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

  val columnMap = SeqMap(
    "aaf103" -> "村(社区)",
    "aac002" -> "公民身份号码",
    "aac003" -> "姓名",
    "aac004" -> "性别",
    "aae002" -> "费款所属期",
    "aae208" -> "实付年月",
    "aae117" -> "支付标志",
    "aae019" -> "总额",
    "aae191" -> "基础养老金中央",
    "aae0192" -> "基础养老金省级",
    "aae0193" -> "基础养老金市级",
    "aae0194" -> "基础养老金县级",
    "aae0195" -> "基础养老金加发",
    "aae0196" -> "丧葬费",
    "aae0201" -> "个账个人缴费",
    "aae0202" -> "个账省补贴",
    "aae0203" -> "个账市补贴",
    "aae0204" -> "个账县补贴",
    "aae0207" -> "个账代缴",
    "aae0205" -> "个账集体补助",
    "aae0208" -> "个账被征地",
    "aae0213" -> "个账退捕渔民",
    "aae0214" -> "个账核工业",
  )
}
