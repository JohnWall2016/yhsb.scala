package yhsb.cjb.db

import io.getquill._
import yhsb.cjb.net.protocol.JBKind

object FullCover {
  lazy val fullcover = new MysqlJdbcContext(LowerCase, "fullcover2020")
  import fullcover._

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
      wqjbsf: Option[String] // 我区参加居保身份
  ) {
    def suggestHsqk: (String, String) = {

      // 已参加居保: 1
      val cbsf = (s: String) => s"参保身份: $s"
      if (inSfwqjb == Option("1")) {
        return ("我区参加居保", cbsf(JBKind.getOrElse(wqjbsf.getOrElse(""), "")))
      }

      // 不参加居保: 0
      val bdjg = (s: String) => s"比对结果: $s"
      slcb.getOrElse("") match {
        case "机关事业" => return ("参加机关保", bdjg("省内机关养老保险"))
        case "企业职工" => return ("参加省职保", bdjg("省内职工养老保险"))
        case "城乡居民" => return ("其他人员", bdjg("省内居民养老保险"))
      }

      swcb.getOrElse("") match {
        case "机关事业" => return ("参加机关保", bdjg("省外机关养老保险"))
        case "企业职工" => return ("参加省外职保", bdjg("省外职工养老保险"))
        case "城乡居民" => return ("其他人员", bdjg("省外居民养老保险"))
      }

      val zqls = (s: String) => s"之前落实情况: $s"
      hsqk.getOrElse("") match {
        case s@"16岁以上在校生" => return ("在校学生", zqls(s))
        case s@"参军" => return ("现役军人", zqls(s))
        case s@"服刑人员" => return ("服刑人员", zqls(s))
        case s@"死亡（失踪）" => return ("死亡未销户人员", zqls(s))
        case s@"空挂户" => return ("其他人员", zqls(s))
        case s@"户口不在本地" => return ("其他人员", zqls(s))
        case s@"数据错误" => return ("其他人员", zqls(s))
        
        case s@"参职保（含退休）" => return ("参加省职保", zqls(s))

        case s@"已录入居保" => return ("我区参加居保", zqls(s))
        
        case s@"自愿放弃" => return ("无参保意愿", zqls(s))
      }

      // 无意愿参保: 2
      val hjzt = (s: String) => s"户籍状态: $s"
      manageName.getOrElse("") match {
        case s@"迁出" => return ("其他人员", hjzt(s))
        case s@"注销" => return ("其他人员", hjzt(s))
        case s@"死亡" => return ("死亡未销户人员", hjzt(s))
      }

      inZxxssj.getOrElse("") match {
        case "1" => return ("在校学生", "高校学生")
      }

      ("", "")
    }
  }

  val fc2Stxfsj = quote {
    querySchema[FC2Stxfsj](
      "fc2_stxfsj",
      _.manageCode -> "manage_code",
      _.manageName -> "manage_name",
      _.inFcbooks -> "in_fcbooks",
      _.inQgbdjg -> "in_qgbdjg",
      _.inZxxssj -> "in_zxxssj",
      _.inSfwqjb -> "in_sfwqjb"
    )
  }
}
