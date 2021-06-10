package yhsb.cjb.net.protocol

import scala.collection.SeqMap

/** 到龄待遇核定查询 */
class RetiringPersonQuery(
    retireDate: String, //yyyy-MM-dd
    inArrear: String = "1",
    idCard: String = "",
    cbState: String = "1",
    lifeCert: String = "",
    sortOptions: Map[String, String] = Map(
      "dataKey" -> "xzqh",
      "sortDirection" -> "ascending"
    )
) extends PageRequest[RetiringPersonQuery.Item](
    "dyryQuery",
    sortOptions = sortOptions
  ) {
  /** 乡镇（街道） */
  val aaf013 = ""
  /** 村（社区） */
  val aaf030 = ""

  /** 
   * 预算到龄日期
   * yyyy-MM-dd
   **/
  @JsonName("dlny")
  val retireDate_ = retireDate

  /** 预算后待遇起始时间 */
  val yssj = "1"

  /** 户籍性质 */
  val aac009 = ""

  /**
   * 是否欠费
   * 1: 欠费
   * 2: 不欠费
   */
  @JsonName("qfbz")
  val inArrear_ = inArrear

  @JsonName("aac002")
  val idCard_ = idCard

  /**
   * 参保状态
   * 1: 正常参保
   * 2: 暂停参保
   */
  @JsonName("aac008")
  val cbState_ = cbState

  /** 
   * 是否和社保比对
   * 1: 是
   * 2: 否
   */
  val sb_type = "1"

  /** 
   * 是否生存认证
   * 1: 是
   * 2: 否
   */
  val scrz_type = lifeCert
}

object RetiringPersonQuery {
  def apply(
    retireDate: String, //yyyy-MM-dd
    inArrear: String = "1",
    idCard: String = "",
    cbState: String = "1",
    lifeCert: String = "",
  ) = new RetiringPersonQuery(
    retireDate,
    inArrear,
    idCard,
    cbState,
    lifeCert
  )

  case class Item()

  val columnMap = SeqMap(
    "xzqh" -> "行政区划",
    "aac009" -> "户籍性质",
    "xm" -> "姓名",
    "sfz" -> "证件号码",
    "xb" -> "性别",
    "csrq" -> "出生日期",
    "mz" -> "民族",
    "grcbrq" -> "个人参保日期",
    "rycbzt" -> "个人参保状态",
    "gjnx" -> "共缴年限",
    "lqny" -> "待遇领取年月",
    "bz" -> "备注",
    "qysb_type" -> "是否在社保参保",
    "scrz" -> "是否生存认证"
  )
}