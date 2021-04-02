package yhsb.qb.net.protocol

import yhsb.base.xml._

/** 省内参保人员查询 */
class JoinedPersonInProvinceQuery(
    idCard: String
) extends ClientSql[JoinedPersonInProvinceQuery.Item](
    "F00.01.03",
    "F27.06",
    s"( aac002 = &apos;$idCard&apos;)"
  )

object JoinedPersonInProvinceQuery {
  def apply(idCard: String) = new JoinedPersonInProvinceQuery(idCard)

  case class Item(
      /** 个人编号 */
      @Attribute("sac100")
      var pid: String,
      @Attribute("aac002")
      var idCard: String,
      @Attribute("aac003")
      var name: String,
      @Attribute("aac008")
      var sbState: SBState,
      @Attribute("aac031")
      var cbState: CBState,
      @Attribute("sac007")
      var jfKind: JFKind,
      /** 社保机构名称 */
      @Attribute("aab300")
      var agencyName: String,
      /** 单位编号 */
      @Attribute("sab100")
      var companyCode: String
  )
}
