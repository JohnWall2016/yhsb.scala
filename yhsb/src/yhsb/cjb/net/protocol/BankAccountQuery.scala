package yhsb
package cjb.net.protocol

/** 人员银行账号情况查询 */
case class BankAccountQuery(
    idCard: String
) extends PageRequest[BankAccountQuery.Item]("yhzhcxOpenList") {
  val aaf013 = ""
  val aaz070 = ""
  val aaf101 = ""
  val bie013 = ""
  
  @JsonName("aac002") 
  val idCard_ = idCard
  
  val aae010 = ""
  val aae140 = "170"
  val batchid = ""
  val aae617 = ""
}

object BankAccountQuery {
  def apply(idCard: String) = new BankAccountQuery(idCard)

  case class Item(
      /** 社保卡标志 */
      @JsonName("aae617")
      ssCardFlag: String,
  )
}