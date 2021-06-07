package yhsb
package cjb.net.protocol

/** 省内参保信息查询 - 银行信息 */
case class BankInfoQuery(
    @JsonName("aac002") idCard: String
) extends Request[BankInfoQuery.Item]("executeSncbgrBankinfoConQ") {
}

object BankInfoQuery {
  case class Item(
      /** 银行类型 */
      @JsonName("bie013")
      bankType: BankType,
      /** 户名 */
      @JsonName("aae009")
      countName: String,
      /** 卡号 */
      @JsonName("aae010")
      cardNumber: String,
      /** 开户时间: yyyy-mm-dd */
      @JsonName("aae036")
      registerTime: String
  )
}