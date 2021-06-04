package yhsb
package cjb.net.protocol

/** 缴费人员终止审核个人信息查询 */
class WorkingPersonStopAuditDetailQuery(
    item: WorkingPersonStopAuditQuery.Item
) extends Request[WorkingPersonStopAuditDetailQuery.Item]("cbzzfhPerinfo") {
  val aaz038 = s"${item.aaz038}"
  val aac001 = s"${item.aac001}"
  val aae160 = item.reason.value

}

object WorkingPersonStopAuditDetailQuery {
  def apply(item: WorkingPersonStopAuditQuery.Item) =
    new WorkingPersonStopAuditDetailQuery(item)

  case class Item(
      @JsonName("aae160")
      reason: StopReason,
      @JsonName("aaz065")
      bankType: BankType
  )
}
