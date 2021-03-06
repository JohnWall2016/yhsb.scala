package yhsb.cjb.net.protocol

class PersonInfoTreatmentAdjustQuery(item: PersonInfoQuery.Item)
  extends PageRequest[PersonInfoTreatmentAdjustQuery.Item](
    "executeGrdybkfsjxx75Q",
    pageSize = 1000,
  ) {
  @JsonName("aac001")
  val pid = item.pid
  val aaz159 = s"${item.aaz159}"
}

object PersonInfoTreatmentAdjustQuery {
  def apply(item: PersonInfoQuery.Item) =
    new PersonInfoTreatmentAdjustQuery(item)

  case class FormMain(
      @JsonName("aae041")
      startYearMonth: String,
      @JsonName("aae036")
      opTime: String,
      @JsonName("aae042")
      endYearMonth: String,
      @JsonName("aae161")
      memo: String, // 核定补发, 生存认证触发待遇续发,..., 待遇终止补发
      @JsonName("aae011")
      operator: String,
      @JsonName("aaa077")
      adjustType: String, // 11: 核定补发, 12: 待遇接续补发, 13: 调整补发, 15: 终止补发

      aae016: String,
      aaz166: String,
      aac001: String,
  )

  case class Item(
      @JsonName("aae041")
      startYearMonth: Int,
      @JsonName("aae036")
      opTime: String,
      @JsonName("aae042")
      endYearMonth: Int,
      @JsonName("aae161")
      memo: String, // 核定补发, 生存认证触发待遇续发,..., 待遇终止补发
      @JsonName("aae011")
      operator: String,
      @JsonName("aaa077")
      adjustType: String, // 11: 核定补发, 12: 待遇接续补发, 13: 调整补发, 15: 终止补发

      aae016: String,
      aaz166: String,
      aac001: String,
  ) {
    def toFormMain = FormMain(
      startYearMonth.toString,
      opTime,
      endYearMonth.toString,
      memo,
      operator,
      adjustType,
      aae016,
      aaz166,
      aac001,
    )
  }
}
