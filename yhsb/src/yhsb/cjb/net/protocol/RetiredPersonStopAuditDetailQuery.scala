package yhsb
package cjb.net.protocol

/** 待遇人员终止审核个人信息查询 */
class RetiredPersonStopAuditDetailQuery(
    item: RetiredPersonStopAuditQuery.Item
) extends Request[RetiredPersonStopAuditDetailQuery.Item]("dyzzfhPerinfo") {
  val aaz176 = s"${item.aaz176}"
}

object RetiredPersonStopAuditDetailQuery {
  case class Item(
      @JsonName("aae160")
      reason: StopReason,
      @JsonName("aaz065")
      bankType: BankType
  )
}
