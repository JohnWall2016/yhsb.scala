package yhsb.qb.net.protocol

import yhsb.base.xml._

/** 单位查询统计 */
class CompanyInfoQuery(
    code: String
) extends ClientSql[CompanyInfoQuery.Item](
    "F00.01.03",
    "F27.01",
    s"( AB01.SAB100 = &apos;$code&apos;)"
  )

object CompanyInfoQuery {
  def apply(id: String) = new CompanyInfoQuery(id)

  case class Item(
      /** 单位编号 */
      @Attribute("sab100")
      var companyCode: String,
      /** 社会保险登记证编码 */
      @Attribute("aab002")
      var registeredCode: String,
      /** 组织机构代码 */
      @Attribute("aab003")
      var organizationCode: String,
      /** 单位名称 */
      @Attribute("aab004")
      var companyName: String,
      @Attribute("aab042")
      var shortCompanyName: String,
      @Attribute("aae006")
      var address: String,
      /** 法人代表 */
      @Attribute("aab013")
      var representativeName: String,
      @Attribute("aab014")
      var representativeIdCard: String,
      @Attribute("aab015")
      var representativePhone: String,
      /** 专管员 */
      @Attribute("aab016")
      var managerName: String,
      @Attribute("sab212")
      var managerIdCard: String,
      @Attribute("sab156")
      var managerPhone: String,
      /** 登记机构 */
      @Attribute("djjg")
      var agencyName: String,
  )
}
