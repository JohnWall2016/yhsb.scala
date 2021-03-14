package yhsb.app.cjb.audit

import yhsb.base.command._
import yhsb.base.datetime.formater.toDashedDate
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbshQuery
import yhsb.cjb.net.protocol.Cbsh
import yhsb.cjb.net.protocol.JBKind
import yhsb.base.excel.Excel
import java.nio.file.Paths
import yhsb.cjb.db.FPData2020._
import yhsb.base.text.Strings.StringOps
import yhsb.base.excel.Excel._

class Audit(args: Seq[String])
    extends Command(args)
    with DateRange
    with Export {
  banner("参保审核与参保身份变更程序")

  val outputDir = """D:\精准扶贫\"""
  val tmplXls = """批量信息变更模板.xls"""

  override def execute(): Unit = {
    val startDate = this.startDate.map(toDashedDate(_)).getOrElse("")
    val endDate = this.endDate.map(toDashedDate(_)).getOrElse("")
    val timeSpan = if (startDate != "") {
      if (endDate != "") {
        s"${startDate}_${endDate}"
      } else {
        startDate
      }
    }
    println(timeSpan)

    val result = Session.use() { sess =>
      sess.sendService(CbshQuery(startDate, endDate))
      sess.getResult[Cbsh]()
    }

    println(s"共计 ${result.size} 条")

    if (!result.isEmpty) {
      val workbook = Excel.load(Paths.get(outputDir, tmplXls))
      val sheet = workbook.getSheetAt(0)
      var index, copyIndex = 1
      var canExport = false

      import fpData2020._

      for (cbsh <- result) {
        val data: List[FPData] = run(
          fphistoryData.filter(_.idcard == lift(cbsh.idcard))
        )
        if (!data.isEmpty) {
          val info = data(0)
          println(
            s"${cbsh.idcard} ${cbsh.name
              .padRight(6)} ${cbsh.birthDay} ${info.jbrdsf.getOrElse("")} " +
              s"${if (info.name != cbsh.name) info.name else ""}"
          )
          val row = sheet.getOrCopyRow(index, copyIndex, false)
          index += 1
          row.getCell("B").setCellValue(cbsh.idcard)
          row.getCell("E").setCellValue(cbsh.name)
          row
            .getCell("J")
            .setCellValue(
              JBKind.invert.getOrElse(info.jbrdsf.getOrElse(""), "")
            )
          canExport = true
        } else {
          println(s"${cbsh.idcard} ${cbsh.name.padRight(6)} ${cbsh.birthDay}")
        }
      }
      if (canExport && export()) {
        println(s"导出 批量信息变更${timeSpan}.xls")
        workbook.save(Paths.get(outputDir, s"批量信息变更${timeSpan}.xls"))
      }
    }
  }
}

object Main {
  def main(args: Array[String]) = new Audit(args).runCommand()
}
