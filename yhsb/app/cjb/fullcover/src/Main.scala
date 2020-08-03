package yhsb.app.cjb.fullcover

import org.rogach.scallop._

import java.nio.file.Paths

import yhsb.util.commands.RowRange
import yhsb.util.commands.InputFile
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.db.FullCover._
import yhsb.util.commands._
import yhsb.util.Strings.StringOps
import yhsb.db.Context.JdbcContextOps
import yhsb.cjb.net.protocol.JBKind

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("全覆盖数据处理程序")

  val updateDwmc = new Subcommand("upDwmc") with InputFile with RowRange {
    descr("更新单位名称")

    val idcardRow = trailArg[String](descr = "身份证列名称")
    val dwxxRow = trailArg[String](descr = "单位信息列名称")
  }

  val importJB = new Subcommand("importJB") with InputFile with RowRange {
    descr("导入居保参保人员明细表")

    val clear =
      opt[Boolean](descr = "是否清除数据表", required = false, default = Some(false))
  }

  val updateZhqk = new Subcommand("upZhqk") {
    descr("根据已记录数据更新综合情况")

    val clear =
      opt[Boolean](descr = "更新老数据", required = false, default = Some(false))
  }

  val downloadByDwmc = new Subcommand("downByDw") {
    descr("按单位导出下发数据")

    val outputDir = opt[String](
      name = "out",
      short = 'o',
      descr = "文件导出路径",
      default = Option("""D:\参保管理\参保全覆盖2\导出目录""")
    )

    val dwmc = trailArg[List[String]](
      descr = "导出单位名称, 默认显示可导出单位; ALL - 导出所有单位",
      required = false
    )

    val tmplXlsx = """D:\参保管理\参保全覆盖2\雨湖区全覆盖下发数据清册模板2.xlsx"""
  }

  addSubcommand(updateDwmc)
  addSubcommand(importJB)
  addSubcommand(updateZhqk)
  addSubcommand(downloadByDwmc)

  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)

    conf.subcommand match {
      case Some(conf.updateDwmc)     => updateDwmc(conf)
      case Some(conf.importJB)       => importJB(conf)
      case Some(conf.updateZhqk)     => updateZhqk(conf)
      case Some(conf.downloadByDwmc) => downloadByDwmc(conf)
      case _                         => conf.printHelp()
    }
  }

  def updateDwmc(conf: Conf) = {
    import conf.updateDwmc._
    println(
      s"begin: ${startRow()}, end: ${endRow()}, " +
        s"idcardRow: ${idcardRow()}, dwxxRow: ${dwxxRow()}"
    )

    val workbook = Excel.load(inputFile())
    val sheet = workbook.getSheetAt(0)

    import fullcover._

    val update = (idcard: String, dwxx: String) =>
      run(
        fc2Stxfsj
          .filter(_.idcard == lift(idcard))
          .update(_.dwmc -> lift(Option(dwxx)))
      )

    for (index <- (startRow() - 1) until endRow()) {
      val row = sheet.getRow(index)
      val idcard = row.getCell(idcardRow()).value
      val dwxx = row.getCell(dwxxRow()).value
      println(s"${index + 1} $idcard $dwxx")
      update(idcard, dwxx)
    }
  }

  def importJB(conf: Conf) = {
    import fullcover._
    import conf.importJB._

    if (clear()) {
      println("清除数据表: 居保参保人员明细表")
      run(jbrymx.delete)
    }

    println("开始导入居保参保人员明细表")
    fullcover.loadExcel(
      jbrymx.quoted,
      inputFile(),
      startRow(),
      endRow(),
      Seq("E", "A", "B", "C", "F", "G", "I", "K", "L", "O"),
      printSql = true
    )
    println("结束导入居保参保人员明细表")
  }

  def updateZhqk(conf: Conf) = {
    import fullcover._

    // 是否在校生
    run(
      fc2Stxfsj
        .filter(_.inZxxssj == Some("1"))
        .update(_.zhqk -> Some("在校学生"), _.memo -> Some("高校学生数据"))
    )

    // 户籍状态
    run(
      fc2Stxfsj
        .filter(e => e.manageName == Some("迁出") || e.manageName == Some("注销"))
        .update(
          _.zhqk -> Some("其他人员"),
          e => e.memo -> Some("户籍状态: " + e.manageName)
        )
    )
    run(
      fc2Stxfsj
        .filter(_.manageName == Some("死亡"))
        .update(_.zhqk -> Some("死亡未销户人员"), e => e.memo -> Some("户籍状态: 死亡"))
    )

    // 之前全覆盖落实情况
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("自愿放弃"))
        .update(_.zhqk -> Some("无参保意愿"), _.memo -> Some("之前落实情况: 自愿放弃"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("已录入居保"))
        .update(_.zhqk -> Some("我区参加居保"), _.memo -> Some("之前落实情况: 已录入居保"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("参职保（含退休）"))
        .update(_.zhqk -> Some("参加省职保"), _.memo -> Some("之前落实情况: 参职保（含退休）"))
    )
    run(
      fc2Stxfsj
        .filter(e =>
          e.hsqk == Some("数据错误") || e.hsqk == Some("户口不在本地") || e.hsqk == Some(
            "空挂户"
          )
        )
        .update(
          _.zhqk -> Some("其他人员"),
          e => e.memo -> Some("之前落实情况: " + e.hsqk)
        )
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("死亡（失踪）"))
        .update(_.zhqk -> Some("死亡未销户人员"), _.memo -> Some("之前落实情况: 死亡（失踪）"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("服刑人员"))
        .update(_.zhqk -> Some("服刑人员"), _.memo -> Some("之前落实情况: 服刑人员"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("参军"))
        .update(_.zhqk -> Some("现役军人"), _.memo -> Some("之前落实情况: 参军"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("16岁以上在校生"))
        .update(_.zhqk -> Some("在校学生"), _.memo -> Some("之前落实情况: 16岁以上在校生"))
    )

    // 数据比对结果
    run(
      fc2Stxfsj
        .filter(_.swcb == Some("机关事业"))
        .update(_.zhqk -> Some("参加机关保"), _.memo -> Some("省外机关养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.swcb == Some("企业职工"))
        .update(_.zhqk -> Some("参加省外职保"), _.memo -> Some("省外职工养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.swcb == Some("城乡居民"))
        .update(_.zhqk -> Some("其他人员"), _.memo -> Some("省外居民养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.slcb == Some("机关事业"))
        .update(_.zhqk -> Some("参加机关保"), _.memo -> Some("省内机关养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.slcb == Some("企业职工"))
        .update(_.zhqk -> Some("参加省外职保"), _.memo -> Some("省内职工养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.slcb == Some("城乡居民"))
        .update(_.zhqk -> Some("其他人员"), _.memo -> Some("省内居民养老保险"))
    )

    // 我区参加居保
    run(
      fc2Stxfsj
        .filter(_.inSfwqjb == Some("1"))
        .update(
          _.zhqk -> Some("我区参加居保"),
          e => e.memo -> Some("参保身份: " + e.wqjbsf)
        )
    )

  }

  def downloadByDwmc(conf: Conf) = {
    import conf.downloadByDwmc._

    import fullcover._

    if (dwmc.isEmpty) {
      val result = run(
        fc2Stxfsj.groupBy(_.dwmc).map(e => (e._1, e._2.size))
      )

      var sum: Long = 0
      for ((name, count) <- result) {
        println(f"${name.getOrElse("").padRight(10)} $count%6d")
        sum += count
      }
      println(f"${"合计".padRight(10)} $sum%6d")
    } else {
      var dwmcs = dwmc()
      if (dwmcs(0).toUpperCase() == "ALL") {
        val result = run(
          fc2Stxfsj.groupBy(_.dwmc).map(_._1)
        )
        dwmcs = result.map {
          case None        => null
          case Some(value) => value
        }
      }
      for (dw <- dwmcs) {
        val file = Paths.get(outputDir(), s"${dw}全覆盖下发数据.xlsx")
        println(s"导出 $dw => $file")
        val result: List[FC2Stxfsj] = run(
          fc2Stxfsj
            .filter(_.dwmc == lift(Option(dw)))
            .sortBy(e => (e.address, e.name))
          //.take(10)
        )
        if (!result.isEmpty) {
          val workbook = Excel.load(tmplXlsx)
          val sheet = workbook.getSheetAt(0)
          var index, start = 2
          for (e <- result) {
            println(s"${index - start + 1} ${e.name} ${e.idcard}")
            val row = sheet.getOrCopyRow(index, start)
            row.getCell("A").setCellValue(index - start + 1)
            row.getCell("B").setCellValue(e.name)
            row.getCell("C").setCellValue(e.idcard)
            row.getCell("D").setCellValue(e.address.getOrElse(""))
            row.getCell("E").setCellValue(e.manageName.getOrElse(""))
            row.getCell("F").setCellValue(e.hsqk.getOrElse(""))
            row.getCell("G").setCellValue(e.slcb.getOrElse(""))
            row.getCell("H").setCellValue(e.swcb.getOrElse(""))
            row
              .getCell("I")
              .setCellValue(
                e.inZxxssj
                  .map { case "1" => "是"; case _ => "" }
                  .getOrElse("")
              )
            row
              .getCell("J")
              .setCellValue(
                e.inZxxssj
                  .map { case "1" => "是"; case _ => "" }
                  .getOrElse("")
              )
            row.getCell("K").setCellValue(e.dwmc.getOrElse(""))

            val (qk, memo) = e.suggestZhqk()

            row.getCell("O").setCellValue(qk)
            row.getCell("P").setCellValue(memo)

            index += 1
          }
          workbook.save(file)
        }
      }
    }
  }
}
