package yhsb.cjb.net.protocol

/** 数据比对查询查询 */
class DataCompareQuery(
    idCard: String
) extends PageRequest[DataCompareQuery.Item]("grzhcx_sjbdcxquery") {
  val aab301 = "430302"
  val bdsj = ""

  @JsonName("aac002")
  val idCard_ = idCard
  
  @JsonName("aac003")
  val name = ""

  val jbzt = ""
  val zbzt = ""
}

object DataCompareQuery {
  def apply(idCard: String) = new DataCompareQuery(idCard)
  
  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      @JsonName("lqsj_zb")
      pensionDate: String,
      @JsonName("zbzt")
      zbState: ZbState,
  )
}
