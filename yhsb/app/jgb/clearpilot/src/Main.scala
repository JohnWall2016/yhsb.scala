import scala.collection.mutable

import yhsb.base.command._
import yhsb.base.excel.Excel._
import yhsb.base.io.File._
import yhsb.base.io.Path._
import yhsb.base.text.String.StringOps
import java.nio.file.Files
import java.io.File

class ClearPilot(args: collection.Seq[String]) extends Command(args) {
  banner("原试点清理程序")

  val templateXlsx = """E:\机关养老保险\（模板）试点期间参保人员缴费确认表.xls"""

  val split = new Subcommand("split") with InputFile with RowRange {
    descr("根据新单位分组")

    val dwCol = opt[String](
      name = "dwmc",
      short = 'd',
      descr = "单位名称所在列",
      default = Option("M")
    )

    def execute() = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      val dwMap = mutable.Map[String, mutable.ListBuffer[Int]]()

      for (i <- (startRow() - 1) until endRow()) {
        val dw = sheet.getRow(i).getCell(dwCol()).value.trim()
        if (!dwMap.contains(dw)) {
          dwMap(dw) = mutable.ListBuffer(i)
        } else {
          dwMap(dw).append(i)
        }
      }

      val outputDir = trimExtension(inputFile())
      Files.createDirectory(outputDir)

      val code = sheet.getCell("C3").value
      val name = sheet.getCell("G3").value

      for ((dw, records) <- dwMap) {
        val sname = if (dw == null || dw.isEmpty) "其它" else dw
        val fileName = s"试点期间参保人员缴费确认表_${code}_$name($sname).xls"

        println(s"生成 $fileName")

        val outWorkbook = Excel.load(templateXlsx)
        val outSheet = outWorkbook.getSheetAt(0)

        outSheet.getCell("G3").setCellValue(s"$name($sname)")

        var index, begIndex = 4

        for (i <- records) {
          val row = sheet.getRow(i)
          val outRow = outSheet.getOrCopyRow(index, begIndex)

          for (r <- 1 to 11) {
            if (r == 8 || r == 9) {
              outRow
                .getCell(r)
                .setCellValue(
                  row.getCell(r).getNumericCellValue
                )
            } else if (r == 11) {
              outRow.getOrCreateCell(r).setCellValue("")
            } else {
              outRow
                .getCell(r)
                .setCellValue(
                  row.getCell(r).value
                )
            }
          }

          index += 1
        }

        val outRow = outSheet.getOrCopyRow(index, begIndex)
        outRow
          .getCell("B")
          .setCellFormula(
            s"""CONCATENATE("共 ",SUMPRODUCT(1/COUNTIF(C5:C$index,C5:C$index&"*"))," 人")"""
          )
        outRow.getCell("H").setCellValue("合计")
        outRow
          .getCell("I")
          .setCellFormula(
            s"""SUM(I5:I$index)"""
          )
        outRow
          .getCell("J")
          .setCellFormula(
            s"""SUM(J5:J$index)"""
          )

        outWorkbook.save(outputDir / fileName)
      }
    }
  }

  val convert = new Subcommand("convert") with InputDir {
    descr("转换确认表格式")

    def execute() = {
      val outputDir = inputDir().insertBeforeLast("(新表)")
      Files.createDirectory(outputDir)

      val inputFiles = listFiles(new File(inputDir()), """.*确认表.*\.xls""")

      for (f <- inputFiles) {
        println(s"转换 $f")

        val workbook = Excel.load(f.toPath)
        val sheet = workbook.getSheetAt(0)

        val outWorkbook = Excel.load(templateXlsx)
        var outSheet = outWorkbook.getSheetAt(0)

        val copyRange = 1 to 11
        var startRow, currentRow = 4

        val code = sheet.getCell(2, 2).value
        val name = sheet.getCell(2, 6).value
        outSheet.getCell(2, 2).setCellValue(code)
        outSheet.getCell(2, 6).setCellValue(name)

        println(s"$name $code")

        var break = false

        for (i <- 4 to sheet.getLastRowNum if !break) {
          val row = sheet.getRow(i)
          val startCell = row.getCell(copyRange.start)
          if (startCell != null) {
            val id = startCell.value
            if (id.matches("""^\d+$""")) {
              println(s"$currentRow $id")
              val outRow = outSheet.getOrCopyRow(currentRow, startRow)
              outRow.getCell(copyRange.start).setCellValue(id)

              for (r <- copyRange.drop(1)) {
                if (r == 8 || r == 9) {
                  outRow
                    .getCell(r)
                    .setCellValue(
                      row.getCell(r).getNumericCellValue
                    )
                } else {
                  outRow
                    .getCell(r)
                    .setCellValue(
                      row.getCell(r).value
                    )
                }
              }
              currentRow += 1
            } else if (id == "说明：") {
              val outRow = outSheet.getOrCopyRow(currentRow, startRow)
              outRow
                .getCell("B")
                .setCellFormula(
                  s"""CONCATENATE("共 ",SUMPRODUCT(1/COUNTIF(C5:C$currentRow,C5:C$currentRow&"*"))," 人")"""
                )
              outRow.getCell("H").setCellValue("合计")
              outRow
                .getCell("I")
                .setCellFormula(
                  s"""SUM(I5:I$currentRow)"""
                )
              outRow
                .getCell("J")
                .setCellFormula(
                  s"""SUM(J5:J$currentRow)"""
                )
              break = true
            }
          }
        }
        outWorkbook.save(
          outputDir / s"试点期间参保人员缴费确认表_${code}_$name.xls"
        )
      }
    }
  }

  addSubCommand(split)
  addSubCommand(convert)
}

object Main {
  def main(args: Array[String]) = new ClearPilot(args).runCommand()
}
