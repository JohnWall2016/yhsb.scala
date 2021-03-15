package yhsb.cjb.db

import io.getquill._

/** 扶贫数据表 */
case class FPData(
    no: Int, // 序号
    xzj: Option[String], // 乡镇街
    csq: Option[String], // 村社区
    address: Option[String], // 地址
    name: String, // 姓名
    idcard: String, // 身份证号码
    birthDay: Option[String], // 出生日期
    pkrk: Option[String], // 贫困人口
    pkrkDate: Option[String], // 贫困人口日期
    tkry: Option[String], // 特困人员
    tkryDate: Option[String], // 特困人员日期
    qedb: Option[String], // 全额低保人员
    qedbDate: Option[String], // 全额低保人员日期
    cedb: Option[String], // 差额低保人员
    cedbDate: Option[String], // 差额低保人员日期
    yejc: Option[String], // 一二级残疾人员
    yejcDate: Option[String], // 一二级残疾人员日期
    ssjc: Option[String], // 三四级残疾人员
    ssjcDate: Option[String], // 三四级残疾人员日期
    sypkry: Option[String], // 属于贫困人员
    jbrdsf: Option[String], // 居保认定身份
    jbrdsfFirstDate: Option[String], // 居保认定身份最初日期
    jbrdsfLastDate: Option[String], // 居保认定身份最后日期
    jbcbqk: Option[String], // 居保参保情况
    jbcbqkDate: Option[String] // 居保参保情况日期
)

trait FPHistoryData { this: MysqlJdbcContext[_] =>
  val fphistoryData = quote {
    querySchema[FPData](
      "fphistorydata"
    )
  }
}

object FPData2020
    extends MysqlJdbcContext(LowerCase, "jzfp2020")
    with FPHistoryData

object FPData2021
    extends MysqlJdbcContext(LowerCase, "jzfp2021")
    with FPHistoryData
