package yhsb.app.cjb.audit

import yhsb.base.command._
import yhsb.base.datetime.formater.toDashedDate
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._
import yhsb.base.io.PathOps._
import yhsb.base.text.Strings.StringOps
import yhsb.base.util.RichOps
import yhsb.cjb.db._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.JBKind
import yhsb.cjb.net.protocol.JoinAuditQuery
import scala.collection.mutable

class Audit(args: Seq[String])
    extends Command(args)
    with DateRange
    with Export {
  banner("参保审核与参保身份变更程序")

  val outputDir = """D:\特殊缴费"""
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
      sess.sendService(JoinAuditQuery(startDate, endDate))
      sess.getResult[JoinAuditQuery#Item]()
    }
    println(s"共计 ${result.size} 条")


    if (!result.isEmpty) {
      case class Item(idCard: String, name: String, jbKind: String)
      val items = mutable.ListBuffer[Item]()

      import FPData2021._

      for (cbsh <- result) {
        val message = s"${cbsh.idcard} ${cbsh.name.padRight(6)} ${cbsh.birthDay}"
        val data: List[FPData] = run(
          fphistoryData.filter(_.idcard == lift(cbsh.idcard))
        )
        data.headOption match {
            case Some(v) =>
              println(
                s"$message ${v.jbrdsf.getOrElse("")} " +
                s"${if (v.name != cbsh.name) v.name else ""}"
              )
              items.addOne(Item(cbsh.idcard, cbsh.name, v.jbcbqk.getOrElse("")))
            case None => println(message)
        }
      }

      if (export() && items.nonEmpty) {
        val workbook = Excel.load(outputDir / tmplXls)
        val sheet = workbook.getSheetAt(0)
        var index, copyIndex = 1

        items.foreach { item =>
          sheet.getOrCopyRow(index, copyIndex, false).let { row =>
              row("B").value = item.idCard
              row("E").value = item.name
              row("J").value = JBKind.invert.getOrElse(item.jbKind, "")
              index += 1
            }
        }

        println(s"导出 批量信息变更${timeSpan}.xls")
        workbook.save(outputDir / s"批量信息变更${timeSpan}.xls")
      }
    }
  }
}

object Main {
  def main(args: Array[String]) = new Audit(args).runCommand()
}
