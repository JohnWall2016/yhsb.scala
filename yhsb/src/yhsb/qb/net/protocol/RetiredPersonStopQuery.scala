package yhsb.qb.net.protocol

import yhsb.base.xml._

/** 离退休终止人员查询 */
class RetiredPersonStopQuery(
    idCard: String
) extends ClientSql[RetiredPersonStopQuery.Item](
    "F00.01.03",
    "Q06.04.02.01",
    s"( AC01.AAC002 = &apos;$idCard&apos;)"
  )

object RetiredPersonStopQuery {
  def apply(idCard: String) = new RetiredPersonStopQuery(idCard)

  case class Item(
      @Attribute("aic162")
      var retiredDate: String,
      @Attribute("aic160")
      var payStartYearMonth: String,
      @Attribute("aic232")
      var stopYearMonth: String,
  )
}
