package yhsb
package cjb.net.protocol

import scala.collection.SeqMap

/** 待遇人员暂停查询 */
class RetiredPersonPauseQuery(
    idCard: String,
    sortOptions: Map[String, String] = Map(
      "dataKey" -> "aaf102",
      "sortDirection" -> "ascending"
    )
) extends PageRequest[RetiredPersonPauseQuery.Item](
      "queryAllPausePersonInfosService",
      sortOptions = sortOptions
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
  def apply(idCard: String = "") = new RetiredPersonPauseQuery(idCard)

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

  val columnMap = SeqMap(
    "aaf102" -> "所在村组",
    "aac009" -> "户籍性质",
    "aac002" -> "证件号码",
    "aac003" -> "姓名",
    "aac004" -> "性别",
    "aae141" -> "暂停年月",
    "aae160" -> "暂停原因",
    "aae013" -> "暂停备注"
  )
}
