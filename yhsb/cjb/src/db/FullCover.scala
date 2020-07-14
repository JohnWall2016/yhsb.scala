package db

import io.getquill._

object FullCover {
  lazy val context = new MysqlJdbcContext(LowerCase, "fullcover2020")
  import context._

  /**
   * 全覆盖2省厅下发数据
   */ 
  case class FC2Stxfsj(
    id: Int,
    idcard: String, // 身份证号码
    name: String, // 姓名
    address: Option[String], // 户籍地址
    manageCode: Option[String], // 管理状态代码
    manageName: Option[String], // 管理状态名称
    inFcbooks: Option[String], // 是否在之前全覆盖落实总台账中 '0'-否, '1'-是
    inQgbdjg: Option[String], // 是否在全国信息比对结果中
    inZxxssj: Option[String], // 是否在在校学生数据中
    inSfwqjb: Option[String], // 是否在我区参加居保
    dwmc: Option[String], // 单位名称
    xfpc: Option[String], // 下发批次("第一批", "第二批", ...)
    wcbyy: Option[String], // 未参保原因
    hsqk: Option[String], // 之前全覆盖落实总台账中 核实情况
    slcb: Option[String], // 省内参保类型: '机关事业', '企业职工', '城乡居民'
    swcb: Option[String], // 省外参保类型: '机关事业', '企业职工', '城乡居民'
  )

  val fc2Stxfsj = quote {
    querySchema[FC2Stxfsj](
      "fc2_stxfsj",
      _.manageCode -> "manage_code",
      _.manageName -> "manage_name",
      _.inFcbooks -> "in_fcbooks",
      _.inQgbdjg -> "in_qgbdjg",
      _.inZxxssj -> "in_zxxssj",
      _.inSfwqjb -> "in_sfwqjb",
    )
  }
}