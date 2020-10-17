package yhsb.app.jgb.clearpilot

import scala.collection.mutable

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.util.Files.trimExtension
import java.nio.file.Files
import java.nio.file.Path

class ClearPilot(args: Seq[String]) extends Command(args) {
  banner("原试点清理程序")

  val templateXlsx = """/Users/wangjiong/Downloads/（模板）试点期间参保人员缴费确认表.xls"""

  addSubCommand {
    new Subcommand("split") with InputFile with RowRange {
      descr("根据新单位分组")

      val dwCol = opt[String](
        name = "dwmc",
        short = 'd',
        descr = "单位名称所在列",
        default = Option("M")
      )

      def execute() {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        val dwMap  = mutable.Map[String, mutable.ListBuffer[Int]]()

        for (i <- (startRow() - 1) until endRow()) {
          val dw = sheet.getRow(i).getCell(dwCol()).value.trim()
          if (!dwMap.contains(dw)) {
            dwMap(dw) = mutable.ListBuffer(i)
          } else {
            dwMap(dw).append(i)
          }
        }

        val outputDir = trimExtension(inputFile())
        Files.createDirectory(Path.of(outputDir))

        val code = sheet.getCell("C3").value
        val name = sheet.getCell("G3").value

        for ((dw, records) <- dwMap) {
          val sname = if (dw == null || dw.isEmpty()) "其它" else dw
          val fileName = s"试点期间参保人员缴费确认表_${code}_$name($sname).xls"

          println(s"生成 $fileName")

          val outWorkbook = Excel.load(templateXlsx)
          val outSheet = outWorkbook.getSheetAt(0)

          outSheet.getCell("G3").setCellValue(s"$name($sname)")

          var index, begIndex = 4

          for (i <- records) {
            val row = sheet.getRow(i)
            val outRow = outSheet.getOrCopyRow(index, begIndex, true)
            
            for (r <- 1 to 11) {
              if (r == 8 || r == 9) {
                outRow.getCell(r).setCellValue(
                  row.getCell(r).getNumericCellValue()
                )
              } else if (r == 11) {
                outRow.getOrCreateCell(r).setCellValue("")
              } else {
                outRow.getCell(r).setCellValue(
                  row.getCell(r).value
                )
              }
            }

            index += 1
          }

          val outRow = outSheet.getOrCopyRow(index, begIndex, true)
          outRow.getCell("B").setCellFormula(
            s"""CONCATENATE("共 ",SUMPRODUCT(1/COUNTIF(C5:C${index},C5:C${index}&"*"))," 人")"""
          )
          outRow.getCell("H").setCellValue("合计")
          outRow.getCell("I").setCellFormula(
            s"""SUM(I5:I${index})"""
          )
          outRow.getCell("J").setCellFormula(
            s"""SUM(J5:J${index})"""
          )

          outWorkbook.save(Path.of(outputDir, fileName))
        }
      }
    }
  }
}

object Main {
  def main(args: Array[String]) = new ClearPilot(args).runCommand()
}