package yhsb.app.cjb.fullcover

import org.rogach.scallop._
import yhsb.util.commands.RowRange
import yhsb.util.commands.InputFile
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.db.FullCover._
import yhsb.util.commands.OutputDir

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("全覆盖数据处理程序")

  val updateDwmc = new Subcommand("upDwmc") with InputFile with RowRange {
    descr("更新单位名称")

    val idcardRow = trailArg[String](descr = "身份证列名称")
    val dwxxRow = trailArg[String](descr = "单位信息列名称")
  }

  val downloadByDwmc = new Subcommand("downByDw") with OutputDir {
    descr("按单位导出下发数据")
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
          println(s"${index+1} $idcard $dwxx")
          update(idcard, dwxx)
        }
      }
      case Some(conf.downloadByDwmc) => {
        import conf.downloadByDwmc._

        import fullcover._

        val result = run(
          fc2Stxfsj.groupBy(_.dwmc).map(e => (e._1, e._2.size))
        )

        for ((name, count) <- result) {
          println(s"$name $count")
        }

      }
      case _ => {
        conf.printHelp()
      }
    }
  }
}
