package yhsb.cjb.net.protocol

class PaymentTerminateQuery(
    pid: String,
    yearMonth: String, // 2021-01
    reason: StopReason,
    funeralMoney: String
) extends Request[PaymentTerminateQuery.Item]("dyzzAccrualPro") {
  @JsonName("aae160")
  val reason_ = reason

  @JsonName("aac001")
  val pid_ = pid

  /** 终止日期 */
  @JsonName("aic301")
  val yearMonth_ = yearMonth

  /** 是否有丧葬费: 1-是 2-否 */
  @JsonName("sf")
  val funeralMoney_ = funeralMoney
}

object PaymentTerminateQuery {
  def apply(
      item: PersonInfoQuery.Item,
      yearMonth: String, // 2021-01
      reason: StopReason,
      funeralMoney: String = "2"
  ) = new PaymentTerminateQuery(
    item.pid.toString,
    yearMonth,
    reason,
    funeralMoney
  )

  case class Item(
      @JsonName("aae427")
      auditAmount: String, // 应稽核金额
      @JsonName("aae425")
      payAmount: String // 退款总金额
  )
}
