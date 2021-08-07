package yhsb.cjb.net.protocol

class RefundQuery(
    idCard: String
) extends PageRequest[RefundQuery.Item](
    "jhtkdjcxPerInfoList",
    pageSize = 100
  ) {
  val aaf013 = ""
  val aaf030 = ""

  @JsonName("aac002")
  val idCard_ = idCard

  val aac003 = ""
  val aae153 = ""
  val aae153s = ""
  val aac009 = ""
  val aaa036 = ""
  val aaa171 = ""
}

object RefundQuery {
  def apply(idCard: String) = new RefundQuery(idCard)

  case class Item(
    @JsonName("aae019")
    amount: BigDecimal,
    @JsonName("dzbz")
    state: String, // 已到账
    @JsonName("aae036")
    refundedTime: String,
  )
}
