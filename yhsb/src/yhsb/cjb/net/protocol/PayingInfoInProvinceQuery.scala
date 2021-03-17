package yhsb.cjb.net.protocol

/** 省内参保信息查询 - 缴费信息 */
case class PayingInfoInProvinceQuery(
    @JsonName("aac002") idcard: String
) extends PageRequest[PayingInfoInProvinceQuery.Item](
      "executeSncbqkcxjfxxQ",
      pageSize = 500
    )

object PayingInfoInProvinceQuery {
  case class Item(
      @JsonName("aae003") year: Int, // 缴费年度
      @JsonName("aae013") memo: String, // 备注
      @JsonName("aae022") amount: JBigDecimal, // 金额
      @JsonName("aaa115") typ: JFType, // 缴费类型
      @JsonName("aae341") item: JFItem, // 缴费项目
      @JsonName("aab033") method: JFMethod, // 缴费方式
      @JsonName("aae006") payedOffDay: String, // 划拨日期
      @JsonName("aaa027") agency: String, // 社保机构
      @JsonName("aaf101") xzqhCode: String // 行政区划代码
  ) extends Jsonable {
    def isPayedOff = payedOffDay != null
  }
}
