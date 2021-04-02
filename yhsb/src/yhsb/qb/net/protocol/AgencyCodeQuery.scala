package yhsb.qb.net.protocol

import yhsb.base.xml.Attribute

/** 社保机构编号查询 */
case class AgencyCodeQuery()
  extends FunctionID[AgencyCodeQuery.Item]("F00.01.02", "F28.02")

object AgencyCodeQuery {
  case class Item(
      @Attribute("aab300")
      var name: String,
      @Attribute("aab034")
      var code: String,
  )
}
