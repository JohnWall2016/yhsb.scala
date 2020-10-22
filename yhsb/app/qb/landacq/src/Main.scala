package yhsb.app.qb.landacq

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbxxRequest
import yhsb.cjb.net.protocol.Cbxx
import yhsb.util.Files.appendToFileName
import java.nio.file.Paths
import scala.util.matching.Regex

class LandAcq(args: Seq[String]) extends Command(args) {

  banner("征地农民处理程序")

  val dump = new Subcommand("dump") with InputFile {
    descr("导出特定单位数据")

    val outDir = """D:\征地农民"""
    val template = "雨湖区被征地农民数据模板.xlsx"

    val dwName = trailArg[String](descr = "导出单位名称")

    val filter =
      opt[String](required = false, descr = "过滤单位名称")

    def execute() = {

      val inWorkbook = Excel.load(inputFile())
      val inSheet = inWorkbook.getSheetAt(1)

      val outWorkbook = Excel.load(Paths.get(outDir, template))
      val outSheet = outWorkbook.getSheetAt(0)

      var index, copyIndex = 1

      val regex = if (filter.isDefined) filter() else dwName()
      val mat = new Regex(s".*((万楼)|(护潭)).*") //new Regex(s".*$regex.*")

      for (i <- 3 to inSheet.getLastRowNum()) {
        val inRow = inSheet.getRow(i)
        val xcz = inRow.getCell("U").value

        println(s"$i $xcz")

        if (mat.matches(xcz)) {
          val no = inRow.getCell("A").value
          val project = inRow.getCell("D").value
          val name = inRow.getCell("F").value
          var idcard = inRow.getCell("J").value

          idcard = idcard.trim().toUpperCase()
          val sex = if (idcard.length() == 18) {
            val c = idcard.substring(16, 17)
            if (c.toInt % 2 == 1) "男" else "女"
          } else ""

          val memo = xcz

          val outRow = outSheet.getOrCopyRow(index, copyIndex, false)
          outRow.getCell("A").setCellValue(index)
          outRow.getCell("B").setCellValue(no)
          outRow.getCell("C").setCellValue(project)
          outRow.getCell("D").setCellValue(name)
          outRow.getCell("E").setCellValue(sex)
          outRow.getCell("F").setCellValue(idcard)
          outRow.getCell("G").setCellValue(memo)

          index += 1
        }
      }

      outWorkbook.save(Paths.get(outDir, s"${dwName()}被征地农民数据.xlsx"))
    }
  }

  val jbstate =
    new Subcommand("jbstate") with InputFile with SheetIndexOpt with RowRange {
      descr("查询保存居保状态")

      val nameCol = trailArg[String](descr = "姓名所在列")

      val idcardCol = trailArg[String](descr = "身份号码所在列")

      val resultCol = trailArg[String](descr = "居保状态回写列")

      val retireTime = trailArg[Int](descr = "退休时间")

      def execute() = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(sheetIndex())

        Session.use() { sess =>
          for (i <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(i)
            val name = row.getCell(nameCol()).value.trim()
            val idcard = row.getCell(idcardCol()).value.trim().toUpperCase()

            if (idcard.length() == 18) {
              val male = (idcard.substring(16, 17).toInt % 2) == 1
              val birthMonth = idcard.substring(6, 12).toInt

              println(s"$name $idcard $male $birthMonth")

              if (
                (male && (birthMonth + 6000 <= retireTime()))
                || (!male && (birthMonth + 5500 <= retireTime()))
              ) {
                sess.sendService(CbxxRequest(idcard))
                val result = sess.getResult[Cbxx]()
                val jbState = {
                  val cbxx = result(0)
                  if (name == cbxx.name || cbxx.name == null) cbxx.jbState
                  else s"${cbxx.jbState}(${cbxx.name})"
                }
                println(jbState)
                row
                  .getOrCreateCell(resultCol())
                  .setCellValue(jbState)
              }
            }
          }
        }

        workbook.save(appendToFileName(inputFile(), ".up"))
      }
    }

  addSubCommand(dump)
  addSubCommand(jbstate)
}

object Main {
  def main(args: Array[String]) = new LandAcq(args).runCommand()
}
