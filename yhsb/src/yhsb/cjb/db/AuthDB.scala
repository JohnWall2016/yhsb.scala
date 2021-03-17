package yhsb.cjb.db

import io.getquill._

/** 认证数据表 */
case class AuthItem(
    no: Int, // 序号
    neighborhood: Option[String], // 乡镇街
    community: Option[String], // 村社区
    address: Option[String], // 地址
    name: String, // 姓名
    idCard: String, // 身份证号码
    birthDay: Option[String], // 出生日期
    poverty: Option[String], // 贫困人口
    povertyDate: Option[String], // 贫困人口日期
    veryPoor: Option[String], // 特困人员
    veryPoorDate: Option[String], // 特困人员日期
    fullAllowance: Option[String], // 全额低保人员
    fullAllowanceDate: Option[String], // 全额低保人员日期
    shortAllowance: Option[String], // 差额低保人员
    shortAllowanceDate: Option[String], // 差额低保人员日期
    primaryDisability: Option[String], // 一二级残疾人员
    primaryDisabilityDate: Option[String], // 一二级残疾人员日期
    secondaryDisability: Option[String], // 三四级残疾人员
    secondaryDisabilityDate: Option[String], // 三四级残疾人员日期
    isDestitute: Option[String], // 属于贫困人员
    jbKind: Option[String], // 居保认定身份
    jbKindFirstDate: Option[String], // 居保认定身份最初日期
    jbKindLastDate: Option[String], // 居保认定身份最后日期
    jbState: Option[String], // 居保参保情况
    jbStateDate: Option[String] // 居保参保情况日期
)

trait HistoryData {
  this: MysqlJdbcContext[_] =>
  val historyData = quote {
    querySchema[AuthItem](
      "fphistorydata",
      _.no -> "no",
      _.neighborhood -> "xzj",
      _.community -> "csq",
      _.address -> "address",
      _.name -> "name",
      _.idCard -> "idcard",
      _.birthDay -> "birthDay",
      _.poverty -> "pkrk",
      _.povertyDate -> "pkrkDate",
      _.veryPoor -> "tkry",
      _.veryPoorDate -> "tkryDate",
      _.fullAllowance -> "qedb",
      _.fullAllowanceDate -> "qedbDate",
      _.shortAllowance -> "cedb",
      _.shortAllowanceDate -> "cedbDate",
      _.primaryDisability -> "yejc",
      _.primaryDisabilityDate -> "yejcDate",
      _.secondaryDisability -> "ssjc",
      _.secondaryDisabilityDate -> "ssjcDate",
      _.isDestitute -> "sypkry",
      _.jbKind -> "jbrdsf",
      _.jbKindFirstDate -> "jbrdsfFirstDate",
      _.jbKindLastDate -> "jbrdsfLastDate",
      _.jbState -> "jbcbqk",
      _.jbStateDate -> "jbcbqkDate"
    )
  }
}

object AuthData2020
    extends MysqlJdbcContext(LowerCase, "jzfp2020")
    with HistoryData

object AuthData2021
    extends MysqlJdbcContext(LowerCase, "jzfp2021")
    with HistoryData
