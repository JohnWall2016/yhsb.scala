package yhsb.qb.net.protocol

import yhsb.base.xml._

/** 离退休人员参保查询统计 */
class RetiredPersonQuery(
    idCard: String,
    agencyCode: String
) extends ClientSql[RetiredPersonQuery.Item](
    "F00.01.03",
    "F27.03",
    s"( v.aac002 = &apos;${idCard}&apos;)"
) {
    @AttrNode("para", "aab034")
    val agencyCode_ = agencyCode
}

object RetiredPersonQuery {
  def apply(idCard: String, agencyCode: String) =
    new RetiredPersonQuery(idCard, agencyCode)

  case class Item(
    @Attribute("aab004")
    var companyName: String,

    /** 待遇发放姿态 */
    @Attribute("aae116")
    var payState: String,

    /** 离退休日期 */
    @Attribute("aic162")
    var retireDate: String,

    /** 待遇开始时间 */
    @Attribute("aic160")
    var startTime: String,

    /** 退休金 */
    @Attribute("txj")
    var pension: BigDecimal,
  )
}