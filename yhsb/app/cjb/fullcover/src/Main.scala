package yhsb.app.cjb.fullcover

import org.rogach.scallop._
import yhsb.util.commands.RowRange
import yhsb.util.commands.InputFile
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.db.FullCover._
import yhsb.util.commands._
import yhsb.util.Strings.StringOps
import java.nio.file.Paths

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("全覆盖数据处理程序")

  val updateDwmc = new Subcommand("upDwmc") with InputFile with RowRange {
    descr("更新单位名称")

    val idcardRow = trailArg[String](descr = "身份证列名称")
    val dwxxRow = trailArg[String](descr = "单位信息列名称")
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
  addSubcommand(downloadByDwmc)

  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)

    conf.subcommand match {
      case Some(conf.updateDwmc) => {
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
      case Some(conf.downloadByDwmc) => {
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
              case None => null
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

                index += 1
              }
              workbook.save(file)
            }
          }
        }
      }
      case _ => {
        conf.printHelp()
      }
    }
  }
}
