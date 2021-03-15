package yhsb.cjb.net.protocol

class JoinAuditQuery(
    startDate: String, // "2019-04-29"
    endDate: String,
    shState: String // "1"
) extends PageRequest(
      "cbshQuery",
      pageSize = 500
    ) {
  val aaf013: String = ""
  val aaf030: String = ""
  val aae011: String = ""
  val aae036: String = ""
  val aae036s: String = ""
  val aae014: String = ""
  val aac009: String = ""
  val aac002: String = ""
  val aac003: String = ""
  val sfccb: String = ""

  val aae015 = startDate
  val aae015s = endDate
  val aae016 = shState

  case class Item(
      @JsonName("aac002") idcard: String,
      @JsonName("aac003") name: String,
      @JsonName("aac006") birthDay: String
  ) extends Jsonable
}

object JoinAuditQuery {
  def apply(
      startDate: String, // "2019-04-29"
      endDate: String = "",
      shState: String = "1" // "1"
  ) = new JoinAuditQuery(startDate, endDate, shState)
}
