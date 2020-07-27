package yhsb.app.cjb.fullcover

import org.rogach.scallop._
import yhsb.util.commands.RowRange
import yhsb.util.commands.InputFile
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.db.FullCover._

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("全覆盖数据处理程序")

  val updateDwxx = new Subcommand("upDwxx") with InputFile with RowRange {
    descr("更新单位信息")

    val idcardRow = trailArg[String](descr = "身份证列名称")
    val dwxxRow = trailArg[String](descr = "单位信息列名称")
  }
  addSubcommand(updateDwxx)
  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)

    conf.subcommand match {
      case Some(conf.updateDwxx) => {
        import conf.updateDwxx._
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
      case _ => {
        conf.printHelp()
      }
    }
  }
}
