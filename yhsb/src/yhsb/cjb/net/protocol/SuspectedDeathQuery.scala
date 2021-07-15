package yhsb.cjb.net.protocol

/** 疑似死亡人员信息查询 */
class SuspectedDeathQuery(
    idCard: String
) extends PageRequest[SuspectedDeathQuery.Item]("dsznswcxQuery") {
  @JsonName("aac003")
  val name = ""

  @JsonName("aae037s")
  val startDeathDate = ""

  @JsonName("aae037e")
  val endDeathDate = ""

  @JsonName("aac002")
  val idCard_ = idCard

  val aaf013 = ""
  val aaf101 = ""
  val aac008 = ""
  val hsbz = ""
}

object SuspectedDeathQuery {
  def apply(idCard: String) = new SuspectedDeathQuery(idCard)
  
  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      @JsonName("aae037")
      deathDate: Int
  )
}
