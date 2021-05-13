package yhsb.cjb.net.protocol

class JoinTerminateQuery(
    pid: String,
    yearMonth: String, // 2021-01
    reason: StopReason,
    funeralMoney: String,
    houseHold: String
) extends Request[JoinTerminateQuery.Item]("cbzzAccrualPro") {
  @JsonName("aae160")
  val reason_ = reason

  @JsonName("aac001")
  val pid_ = pid

  /** 终止日期 */
  @JsonName("aic301")
  val yearMonth_ = yearMonth

  val aac009 = houseHold

  /** 是否有丧葬费: 1-是 2-否 */
  @JsonName("sf")
  val funeralMoney_ = funeralMoney
}

object JoinTerminateQuery {
  def apply(
      item: PersonInfoQuery.Item,
      yearMonth: String, // 2021-01
      reason: StopReason,
      funeralMoney: String = "2"
  ) = new JoinTerminateQuery(
    item.pid.toString,
    yearMonth,
    reason,
    funeralMoney,
    item.houseHold
  )

  case class Item(
      @JsonName("aae028")
      payAmount: String // 退款总金额
  )
}
