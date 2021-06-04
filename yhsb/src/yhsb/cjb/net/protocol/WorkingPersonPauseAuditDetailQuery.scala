package yhsb
package cjb.net.protocol

/** 缴费人员暂停审核查询 查看 */
class WorkingPersonPauseAuditDetailQuery(
    item: WorkingPersonPauseAuditQuery.Item
) extends Request[WorkingPersonPauseAuditDetailQuery.Item](
      "viewPauseInfoService"
    ) {
  @JsonName("id")
  val opId = s"${item.id}"
}

object WorkingPersonPauseAuditDetailQuery {
  def apply(
      item: WorkingPersonPauseAuditQuery.Item
  ): WorkingPersonPauseAuditDetailQuery =
    new WorkingPersonPauseAuditDetailQuery(item)

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
