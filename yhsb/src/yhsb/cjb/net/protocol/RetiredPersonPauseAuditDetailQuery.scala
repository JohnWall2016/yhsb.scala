package yhsb
package cjb.net.protocol

/** 待遇人员暂停审核查询 查看 */
class RetiredPersonPauseAuditDetailQuery(
    item: RetiredPersonPauseAuditQuery#Item
) extends Request("viewPayPauseInfoService") {
  val aac002 = item.idCard
  val aaz173 = s"${item.aaz173}"

  case class Item(
      @JsonName("aae160")
      reason: PauseReason,
      @JsonName("aae011")
      operator: String,
      /** 经办时间 */
      @JsonName("aae036")
      opTime: String
  )
}

object RetiredPersonPauseAuditDetailQuery {
  def apply(
    item: RetiredPersonPauseAuditQuery#Item
  ): RetiredPersonPauseAuditDetailQuery =
    new RetiredPersonPauseAuditDetailQuery(item)
}