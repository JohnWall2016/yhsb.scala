package yhsb.cjb.db

import io.getquill._
import yhsb.base.text.String._

/** 认证数据表 */
case class AuthItem(
    var no: Option[Int], // 序号
    var neighborhood: Option[String], // 乡镇街
    var community: Option[String], // 村社区
    var address: Option[String], // 地址
    var name: Option[String], // 姓名
    var idCard: String, // 身份证号码
    var birthDay: Option[String], // 出生日期
    var poverty: Option[String], // 贫困人口
    var povertyDate: Option[String], // 贫困人口日期
    var veryPoor: Option[String], // 特困人员
    var veryPoorDate: Option[String], // 特困人员日期
    var fullAllowance: Option[String], // 全额低保人员
    var fullAllowanceDate: Option[String], // 全额低保人员日期
    var shortAllowance: Option[String], // 差额低保人员
    var shortAllowanceDate: Option[String], // 差额低保人员日期
    var primaryDisability: Option[String], // 一二级残疾人员
    var primaryDisabilityDate: Option[String], // 一二级残疾人员日期
    var secondaryDisability: Option[String], // 三四级残疾人员
    var secondaryDisabilityDate: Option[String], // 三四级残疾人员日期
    var isDestitute: Option[String], // 属于贫困人员
    var jbKind: Option[String], // 居保认定身份
    var jbKindFirstDate: Option[String], // 居保认定身份最初日期
    var jbKindLastDate: Option[String], // 居保认定身份最后日期
    var jbState: Option[String], // 居保参保情况
    var jbStateDate: Option[String] // 居保参保情况日期
) {
  def merge(item: RawItem): Boolean = {
    var changed = false

    def setOption(
      dest: this.type => Option[String]
    )(
      src: RawItem => Option[String]
    )(
      set: Option[String] => Unit
    ) = {
      val d = dest(this)
      val s = src(item)
      if (d.getOrElse("").isEmpty && s.getOrElse("").nonEmpty) {
        set(s)
        changed = true
      }
    }

    def setString(
      dest: this.type => String
    )(
      src: RawItem => String
    )(
      set: String => Unit
    ) = {
      val d = dest(this)
      val s = src(item)
      if (d.isNullOrEmpty && s.nonNullAndEmpty) {
        set(s)
        changed = true
      }
    }

    setOption(_.neighborhood)(_.neighborhood)(neighborhood = _)
    setOption(_.community)(_.community)(community = _)
    setOption(_.address)(_.address)(address = _)
    setOption(_.name)(_.name)(name = _)
    setString(_.idCard)(_.idCard)(idCard = _)
    setOption(_.birthDay)(_.birthDay)(birthDay = _)

    item.personType match {
      case Some("贫困人口") =>
        if (poverty.getOrElse("").isEmpty()) {
          poverty = item.detail
          povertyDate = item.date
          changed = true
        }
        if (isDestitute.getOrElse("").isEmpty()) {
          isDestitute = item.personType
          changed = true
        }
      case Some(_) =>
      case None => 
    }

    changed
  }
}

trait HistoryData { this: MysqlJdbcContext[_] =>
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

case class MonthItem(
    var no: Option[Int], // 序号
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
    jbStateDate: Option[String], // 居保参保情况日期
    month: Option[String]
)

trait MonthData { this: MysqlJdbcContext[_] =>
  val monthData = quote {
    querySchema[MonthItem](
      "fpmonthdata",
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
      _.jbStateDate -> "jbcbqkDate",
      _.month -> "month"
    )
  }
}

/** 原始采集数据 */
case class RawItem(
    /** 序号 */
    var no: Option[Int],
    /** 乡镇街 */
    neighborhood: Option[String],
    /** 村社区 */
    community: Option[String],
    /** 地址 */
    address: Option[String],
    name: Option[String],
    idCard: String,
    birthDay: Option[String],
    /** 人员类型 */
    personType: Option[String],
    /** 类型细节 */
    detail: Option[String],
    /** 数据月份 */
    date: Option[String]
)

trait RawData { this: MysqlJdbcContext[_] =>
  val rawData = quote {
    querySchema[RawItem](
      "fprawdata",
      _.no -> "no",
      _.neighborhood -> "xzj",
      _.community -> "csq",
      _.address -> "address",
      _.name -> "name",
      _.idCard -> "idcard",
      _.birthDay -> "birthDay",
      _.personType -> "type",
      _.detail -> "detail",
      _.date -> "date"
    )
  }
}

object AuthData2020
  extends MysqlJdbcContext(LowerCase, "jzfp2020")
     with HistoryData
     with RawData
     with MonthData

object AuthData2021
  extends MysqlJdbcContext(LowerCase, "jzfp2021")
     with HistoryData
     with RawData
     with MonthData
