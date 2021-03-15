package yhsb
package cjb.net.protocol

/** 缴费人员暂停审核查询 查看 */
class PayingPersonPauseAuditDetailQuery(
    item: PayingPersonPauseAuditQuery#Item
) extends Request("viewPauseInfoService") {
  @JsonName("id")
  val opId = s"${item.id}"

  case class Item(
      @JsonName("aae160")
      val reason: PauseReason,
      @JsonName("aae011")
      val operator: String,
      /** 经办时间 */
      @JsonName("aae036")
      val opTime: String
  )
}
