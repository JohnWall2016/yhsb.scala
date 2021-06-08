package yhsb.cjb.net.protocol

class PersonInfoTreatmentAdjustDetailQuery(
    item: PersonInfoTreatmentAdjustQuery.Item
) extends Request[PersonInfoTreatmentAdjustDetailQuery.Item](
    "executeGrdybkfmx76Q"
  ) {
  @JsonName("form_main")
  val formMain = item.toFormMain
}

object PersonInfoTreatmentAdjustDetailQuery {
  def apply(item: PersonInfoTreatmentAdjustQuery.Item) =
    new PersonInfoTreatmentAdjustDetailQuery(item)

  case class Item(
      @JsonName("aae003")
      payYearMonth: Int,
      @JsonName("aae002")
      accountYearMonth: Int,
      @JsonName("aae129")
      amount: BigDecimal,
      /**
       * 待遇项目
       * 001001: 基础养老金-中央补助, 001002: 基础养老金-省补助,
       * 001003: 基础养老金-市补助, 001004: 基础养老金-县补助,
       * 002XXX: 个人账户养老金
       */
      @JsonName("aaa036")
      adjustType: String,
      /** 补扣标志 */
      @JsonName("aaa078")
      flag: String
  )
}
