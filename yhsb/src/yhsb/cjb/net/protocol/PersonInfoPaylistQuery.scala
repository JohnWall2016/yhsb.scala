package yhsb.cjb.net.protocol

class PersonInfoPaylistQuery(item: PersonInfoQuery.Item)
  extends PageRequest[PersonInfoPaylistQuery.Item](
    "executeGrPayExtendinfoQ",
    pageSize = 1000,
    sortOptions = Map(
      "dataKey" -> "aae003",
      "sortDirection" -> "ascending"
    )
  ) {
  @JsonName("aac001")
  val pid = item.pid
  val aaz159 = s"${item.aaz159}"
}

object PersonInfoPaylistQuery {
  def apply(item: PersonInfoQuery.Item) =
    new PersonInfoPaylistQuery(item)

  case class Item(
      @JsonName("aae003")
      payYearMonth: Int,
      @JsonName("aae208")
      accountYearMonth: Int,
      @JsonName("aaa036s")
      payItem: String, // 基础养老金-市补助,...
      @JsonName("aae117s")
      payState: String, // 已支付,...
      @JsonName("aae019")
      amount: BigDecimal
  )
}
