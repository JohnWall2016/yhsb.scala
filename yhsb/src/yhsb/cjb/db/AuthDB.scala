package yhsb.cjb.db

import io.getquill._
import yhsb.base.text.String._

/** 认证数据项 */
trait AuthItem {
  /** 序号 */
  var no: Option[Int]
  /** 乡镇街 */
  var neighborhood: Option[String]
  /** 村社区 */
  var community: Option[String]
  /** 地址 */
  var address: Option[String]
  /** 姓名 */
  var name: Option[String]
  /** 身份证号码 */
  var idCard: String
  /** 出生日期 */
  var birthDay: Option[String]
  /** 贫困人口 */
  var poverty: Option[String]
  /** 贫困人口日期 */
  var povertyDate: Option[String]
  /** 特困人员 */
  var veryPoor: Option[String]
  /** 特困人员日期 */
  var veryPoorDate: Option[String]
  /** 全额低保人员 */
  var fullAllowance: Option[String]
  /** 全额低保人员日期 */
  var fullAllowanceDate: Option[String]
  /** 差额低保人员 */
  var shortAllowance: Option[String]
  /** 差额低保人员日期 */
  var shortAllowanceDate: Option[String]
  /** 一二级残疾人员 */
  var primaryDisability: Option[String]
  /** 一二级残疾人员日期 */
  var primaryDisabilityDate: Option[String]
  /** 三四级残疾人员 */
  var secondaryDisability: Option[String]
  /** 三四级残疾人员日期 */
  var secondaryDisabilityDate: Option[String]
  /** 属于贫困人员 */
  var isDestitute: Option[String]
  /** 居保认定身份 */
  var jbKind: Option[String]
  /** 居保认定身份最初日期 */
  var jbKindFirstDate: Option[String]
  /** 居保认定身份最后日期 */
  var jbKindLastDate: Option[String]
  /** 居保参保情况 */
  var jbState: Option[String]
  /** 居保参保情况日期   */
  var jbStateDate: Option[String]
    
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

    def setDestitute(
      testField: Option[String]
    )(
      setFields: RawItem => Unit
    ) = {
      if (testField.getOrElse("").isEmpty()) {
        setFields(item)
        changed = true
      }
    }

    item.personType match {
      case Some("贫困人口") =>
        setDestitute(poverty) { it =>
          poverty = it.detail
          povertyDate = it.date
        }
      case Some("特困人员") =>
        setDestitute(veryPoor) { it =>
          veryPoor = it.detail
          veryPoorDate = it.date
        }
      case Some("全额低保人员") =>
        setDestitute(fullAllowance) { it =>
          fullAllowance = it.detail
          fullAllowanceDate = it.date
        }
      case Some("差额低保人员") =>
        setDestitute(shortAllowance) { it =>
          shortAllowance = it.detail
          shortAllowanceDate = it.date
        }
      case Some("一二级残疾人员") =>
        setDestitute(primaryDisability) { it =>
          primaryDisability = it.detail
          primaryDisabilityDate = it.date
        }
      case Some("三四级残疾人员") =>
        setDestitute(secondaryDisability) { it =>
          secondaryDisability = it.detail
          secondaryDisabilityDate = it.date
        }
      case Some(_) =>
      case None => 
    }

    changed
  }
}

case class HistoryItem(
    var no: Option[Int] = None,
    var neighborhood: Option[String] = None,
    var community: Option[String] = None,
    var address: Option[String] = None,
    var name: Option[String] = None,
    var idCard: String = null,
    var birthDay: Option[String] = None,
    var poverty: Option[String] = None,
    var povertyDate: Option[String] = None,
    var veryPoor: Option[String] = None,
    var veryPoorDate: Option[String] = None,
    var fullAllowance: Option[String] = None,
    var fullAllowanceDate: Option[String] = None,
    var shortAllowance: Option[String] = None,
    var shortAllowanceDate: Option[String] = None,
    var primaryDisability: Option[String] = None,
    var primaryDisabilityDate: Option[String] = None,
    var secondaryDisability: Option[String] = None,
    var secondaryDisabilityDate: Option[String] = None,
    var isDestitute: Option[String] = None,
    var jbKind: Option[String] = None,
    var jbKindFirstDate: Option[String] = None,
    var jbKindLastDate: Option[String] = None,
    var jbState: Option[String] = None,
    var jbStateDate: Option[String] = None,
) extends AuthItem

trait HistoryData { this: MysqlJdbcContext[_] =>
  val historyData = quote {
    querySchema[HistoryItem](
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
    var no: Option[Int] = None,
    var neighborhood: Option[String] = None,
    var community: Option[String] = None,
    var address: Option[String] = None,
    var name: Option[String] = None,
    var idCard: String = null,
    var birthDay: Option[String] = None,
    var poverty: Option[String] = None,
    var povertyDate: Option[String] = None,
    var veryPoor: Option[String] = None,
    var veryPoorDate: Option[String] = None,
    var fullAllowance: Option[String] = None,
    var fullAllowanceDate: Option[String] = None,
    var shortAllowance: Option[String] = None,
    var shortAllowanceDate: Option[String] = None,
    var primaryDisability: Option[String] = None,
    var primaryDisabilityDate: Option[String] = None,
    var secondaryDisability: Option[String] = None,
    var secondaryDisabilityDate: Option[String] = None,
    var isDestitute: Option[String] = None,
    var jbKind: Option[String] = None,
    var jbKindFirstDate: Option[String] = None,
    var jbKindLastDate: Option[String] = None,
    var jbState: Option[String] = None,
    var jbStateDate: Option[String] = None,
    var month: Option[String] = None,
) extends AuthItem

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
    var no: Option[Int] = None,
    /** 乡镇街 */
    neighborhood: Option[String] = None,
    /** 村社区 */
    community: Option[String],
    /** 地址 */
    address: Option[String],
    name: Option[String],
    idCard: String,
    birthDay: Option[String],
    /** 人员类型 */
    var personType: Option[String] = None,
    /** 类型细节 */
    var detail: Option[String] = None,
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
