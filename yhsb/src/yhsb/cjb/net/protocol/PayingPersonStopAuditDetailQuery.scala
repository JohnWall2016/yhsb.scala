package yhsb
package cjb.net.protocol

/** 缴费人员终止审核个人信息查询 */
class PayingPersonStopAuditDetailQuery(
    item: PayingPersonStopAuditQuery#Item
) extends Request("cbzzfhPerinfo") {
  val aaz038 = s"${item.aaz038}"
  val aac001 = s"${item.aac001}"
  val aae160 = item.reason.value

  case class Item(
      @JsonName("aae160")
      reason: StopReason,
      @JsonName("aaz065")
      bankType: BankType
  )
}
