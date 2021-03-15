package yhsb.app.cjb.dataverify

import yhsb.base.command._
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._
import yhsb.cjb.net.Session
import scala.collection.mutable
import yhsb.base.text.Strings._
import yhsb.cjb.net.protocol.PersonInfoInProvinceQuery
import yhsb.base.io.PathOps._

class Verify(args: Seq[String])
    extends Command(args)
    with InputFile
    with RowRange
    with OutputDir {
  banner("数据核实程序")

  val template = """D:\数据核查\20201125\湖南省城乡居民基本养老保险信息修改汇总表.xls"""

  object Type extends Enumeration {
    val Jfry = Value("正常缴费人员")
    val Dyry = Value("正常待遇人员")

    def from(name: String): Option[Value] =
      values.find(name == _.toString())
  }

  case class Data(
      idcard: String,
      name: String,
      oldName: String,
      typ: Type.Value,
      memo: String
  )

  override def execute(): Unit = {
    val workbook = Excel.load(inputFile())
    val sheet = workbook.getSheetAt(0)

    println("生成信息修改数据")
    val data = mutable.Map[String, mutable.ListBuffer[Data]]()
    Session.use() { sess =>
      for (r <- (startRow() - 1) until endRow()) {
        val row = sheet.getRow(r)
        val name = row.getCell("C").value.trim
        val idcard = row.getCell("D").value.trim
        println(s"  $name $idcard")
        sess.sendService(PersonInfoInProvinceQuery(idcard))
        val result = sess.getResult[PersonInfoInProvinceQuery#Item]()
        if (!result.isEmpty && result(0).name != name) {
          val cbxx = result(0)
          val typ = Type.from(cbxx.jbState)
          if (typ.isDefined) {
            val dwcs = cbxx.dwAndCsName.get
            if (!data.isDefinedAt(dwcs._1)) {
              data(dwcs._1) = mutable.ListBuffer(
                Data(idcard, name, cbxx.name, typ.get, dwcs._2)
              )
            } else {
              data(dwcs._1) += Data(idcard, name, cbxx.name, typ.get, dwcs._2)
            }
          }
        }
      }
    }

    println("\n按单位导出信息修改数据")
    for (dw <- data.keySet) {
      val workbook = Excel.load(template)
      val sheet = workbook.getSheetAt(0)
      var currentRow, startRow = 3

      sheet.getCell("A2").setCellValue(
        sheet.getCell("A2").value + dw
      )
      
      print(s"  导出 ${dw.padRight(12)}")
      for (dt <- data(dw).sortBy(_.memo)) {
        val row = sheet.getOrCopyRow(currentRow, startRow)
        row.getCell("A").setCellValue(currentRow - startRow + 1)
        row.getCell("B").setCellValue(dt.idcard)
        row.getCell("C").setCellValue(dt.name)
        row.getCell("D").setCellValue(dt.typ.toString())
        row.getCell("E").setCellValue("姓名")
        row.getCell("F").setCellValue(dt.oldName)
        row.getCell("G").setCellValue(dt.name)
        row.getCell("H").setCellValue("居民身份证")
        row.getCell("I").setCellValue(dt.memo)
        currentRow += 1
      }
      println(s" ${currentRow - startRow}")

      workbook.save(outputDir() / s"${dw}.xls")
    }
  }
}

object Main {
  def main(args: Array[String]) = new Verify(args).runCommand()
}
