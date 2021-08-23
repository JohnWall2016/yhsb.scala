package yhsb.cjb.net.protocol

import scala.collection.SeqMap

/** 待遇人员认证查询 */
class CertedPersonQuery(
    idCard: String = "",
    name: String = "",
    startNextCentDate: String = "", // yyyyMM
    endNextCentDate: String = "",
    sortOptions: Map[String, String] = Map(
      "dataKey" -> "aaa129",
      "sortDirection" -> "ascending"
    )
) extends PageRequest[CertedPersonQuery.Item](
    "scrzQuery",
    sortOptions = sortOptions
  ) {
  val aaf013 = ""
  val aaf030 = ""

  @JsonName("aae011")
  val operator = ""

  val aae036 = startNextCentDate

  val aae036s = endNextCentDate

  val aac009 = ""

  @JsonName("aac002")
  val idCard_ = idCard

  @JsonName("aac003")
  val name_ = name

  val aac097 = ""
  val aac098 = ""
}

object CertedPersonQuery {
  def apply(
    idCard: String = "",
    startNextCentDate: String = "", // yyyyMM
    endNextCentDate: String = "",
  ) = new CertedPersonQuery(
    idCard = idCard,
    startNextCentDate = startNextCentDate,
    endNextCentDate = endNextCentDate
  )

  case class Item(
      @JsonName("aac002")
      idCard: String,
      @JsonName("aac003")
      name: String,
      @JsonName("aaa129")
      czName: String,
      /** 下次认证时间 */
      @JsonName("aac099")
      nextCertDate: String,
      /** 认证年月 */
      @JsonName("aac096")
      certedDate: String,
      /** 认证经办时间 */
      @JsonName("aac095")
      certedOpTime: String,
      /** 认证标志 */
      @JsonName("aac097")
      certFlag: CertFlag,
      /** 认证方式 */
      @JsonName("aac098")
      certType: CertType,
      /** 待遇状态 */
      @JsonName("aae116")
      treatmentState: String,
  ) extends DivisionName

  val columnMap = SeqMap(
    "aaa129" -> "行政区划",
    "aac009" -> "户籍性质",
    "aac003" -> "姓名",
    "aac002" -> "证件号码",
    "aac004" -> "性别",
    "aac006" -> "出生日期",
    "aae116" -> "待遇状态",
    "aac098" -> "认证方式",
    "aac099" -> "下次认证时间",
    "aac096" -> "认证年月",
    "aac097" -> "认证标志",
    "aac095" -> "认证经办时间",
  )
}
