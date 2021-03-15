package yhsb.app.cjb.cert

import yhsb.base.command._
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._
import yhsb.base.text.Strings.StringOps
import yhsb.cjb.net.protocol.Division._
import yhsb.base.io.PathOps._

import scala.collection.mutable

import java.nio.file.Files
import java.nio.file.Path

class Cert(args: Seq[String]) extends Command(args) {
  banner("待遇认证数据统计和表格生成程序")

  val split =
    new Subcommand("split") with InputFile with RowRange with OutputDir {

      def xzqhRow() = "A"

      val template = """D:\待遇认证\2020年\城乡居民基本养老保险待遇领取人员资格认证表（表二）.xls"""

      override def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        def createDir(path: Path) = {
          if (!Files.exists(path)) {
            Files.createDirectory(path)
          }
        }

        println(s"生成数据分组")
        val groups = (for (index <- (startRow() - 1) until endRow())
          yield (sheet.getRow(index)(xzqhRow()).value, index)
          ).iterator.groupByDwAndCsName()

        var total = 0
        println(s"导出数据到: ${outputDir()}")
        createDir(Path.of(outputDir()))
        for ((dw, groups) <- groups) {
          println(s"\n${(dw + ":").padRight(11)}   ${groups.size}")
          total += groups.size
          createDir(Path.of(outputDir(), dw))

          for ((cs, indexes) <- groups) {
            println(s"  ${(cs + ":").padRight(11)} ${indexes.size}")
            val outWorkbook = Excel.load(template)
            val outSheet = outWorkbook.getSheetAt(0)

            outSheet.getCell("C2").setCellValue(s"$dw$cs")
            var startRow, currentRow = 4
            for (i <- indexes) {
              val row = sheet.getRow(i)
              val outRow = outSheet.getOrCopyRow(currentRow, startRow, true)
              outRow.getCell("A").setCellValue(currentRow - startRow + 1)
              outRow.getCell("B").setCellValue(row.getCell("C").value)
              outRow
                .getCell("C")
                .setCellValue(
                  if (row.getCell("E").value == "1") "男" else "女"
                )
              outRow.getCell("D").setCellValue(row.getCell("D").value)
              outRow.getCell("E").setCellValue(row.getCell("A").value)
              outRow
                .getCell("M")
                .setCellValue(
                  row
                    .getCell("I")
                    .value
                )
              currentRow += 1
            }

            outWorkbook.save(outputDir() / dw / s"${cs}.xls")
          }
        }
        println(s"\n${"合计:".padRight(11)} $total")
      }
    }

  addSubCommand(split)
}

object Main {
  def main(args: Array[String]) = new Cert(args).runCommand()
}
