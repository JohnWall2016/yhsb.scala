package yhsb
package cjb.net.protocol

/** 待遇人员暂停查询 */
class RetiredPersonPauseQuery(
    idCard: String
) extends PageRequest[RetiredPersonPauseQuery.Item](
      "queryAllPausePersonInfosService"
    ) {
  val aaf013 = ""
  val aaf030 = ""

  @JsonName("aac002")
  val idCard_ = idCard

  val aae141 = ""
  val aae141s = ""

  val aac009 = ""

  val aae036 = ""
  val aae036s = ""
}

object RetiredPersonPauseQuery {
  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      /** 暂停年月? */
      @JsonName("aae002")
      pauseYearMonth: Int,
      /** 暂停原因? */
      @JsonName("aae160")
      reason: PauseReason,
      /** 村组名称 */
      @JsonName("aaf102")
      czName: String,
      @JsonName("aae013")
      memo: String
  ) extends DivisionName
}
