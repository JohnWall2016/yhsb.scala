package yhsb.app.cjb.audit

import yhsb.base.command._
import yhsb.base.datetime.formater.toDashedDate
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._
import yhsb.base.io.PathOps._
import yhsb.base.text.Strings.StringOps
import yhsb.base.util.RichOps
import yhsb.cjb.db.FPData2020._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.Cbsh
import yhsb.cjb.net.protocol.CbshQuery
import yhsb.cjb.net.protocol.JBKind

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
      val workbook = Excel.load(outputDir / tmplXls)
      val sheet = workbook.getSheetAt(0)
      var index, copyIndex = 1
      var canExport = false

      import fpData2020._

      for (cbsh <- result) {
        val data: List[FPData] = run(
          fphistoryData.filter(_.idcard == lift(cbsh.idcard))
        )
        if (data.nonEmpty) {
          val info = data(0)
          println(
            s"${cbsh.idcard} ${cbsh.name.padRight(6)} ${cbsh.birthDay} " +
              s"${info.jbrdsf.getOrElse("")} ${if (info.name != cbsh.name) info.name
              else ""}"
          )
          sheet.getOrCopyRow(index, copyIndex, false).let { row =>
            row("B").value = cbsh.idcard
            row("E").value = cbsh.name
            row("J").value = JBKind.invert.getOrElse(
              info.jbrdsf.getOrElse(""),
              ""
            )
            index += 1
          }
          canExport = true
        } else {
          println(s"${cbsh.idcard} ${cbsh.name.padRight(6)} ${cbsh.birthDay}")
        }
      }
      if (canExport && export()) {
        println(s"导出 批量信息变更${timeSpan}.xls")
        workbook.save(outputDir / s"批量信息变更${timeSpan}.xls")
      }
    }
  }
}

object Main {
  def main(args: Array[String]) = new Audit(args).runCommand()
}
