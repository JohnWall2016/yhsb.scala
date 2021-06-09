import yhsb.base.command._
import yhsb.base.excel.Excel._
import yhsb.base.io.Path._
import yhsb.base.text.String.StringOps
import yhsb.cjb.net.protocol.Division._

import java.nio.file.{Files, Path}

class Cert(args: collection.Seq[String]) extends Command(args) {
  banner("待遇认证数据统计和表格生成程序")

  val split =
    new Subcommand("split") with InputFile with RowRange with OutputDirOpt {

      def DivisionRow() = "A"

      val template = """D:\待遇认证\2020年\城乡居民基本养老保险待遇领取人员资格认证表（表二）.xls"""

      val onlyStatics = opt[Boolean](
        name = "onlyStatics",
        short = 's',
        descr = "只进行统计",
        default = Some(false)
      )

      override def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        def createDir(path: Path) = {
          if (!Files.exists(path)) {
            Files.createDirectory(path)
          }
        }

        if (!onlyStatics()) println(s"生成数据分组")
        val groups = (for (index <- (startRow() - 1) until endRow())
          yield (sheet.getRow(index)(DivisionRow()).value, index))
          .groupByDwAndCsName()

        var total = 0
        if (onlyStatics()) {
          for ((dw, groups) <- groups) {
            val count = groups.values.foldLeft(0)(_ + _.size)
            println(s"\r\n${(dw + ":").padRight(11)}   ${count}")
            total += count
            for ((cs, indexes) <- groups) {
              println(s"  ${(cs + ":").padRight(11)} ${indexes.size}")
            }
          }
          println(s"\r\n${"合计:".padRight(11)} $total")
        } else {
          println(s"导出数据到: ${outputDir()}")
          createDir(outputDir())
          for ((dw, groups) <- groups) {
            val count = groups.values.foldLeft(0)(_ + _.size)
            println(s"\r\n${(dw + ":").padRight(11)}   ${count}")
            total += count
            createDir(outputDir() / dw)

            for ((cs, indexes) <- groups) {
              println(s"  ${(cs + ":").padRight(11)} ${indexes.size}")
              val outWorkbook = Excel.load(template)
              val outSheet = outWorkbook.getSheetAt(0)

              outSheet.getCell("C2").setCellValue(s"$dw$cs")
              var startRow, currentRow = 4
              for (i <- indexes) {
                val row = sheet.getRow(i)
                val outRow = outSheet.getOrCopyRow(currentRow, startRow)
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

              outWorkbook.save(outputDir() / dw / s"$cs.xls")
            }
          }
          println(s"\r\n${"合计:".padRight(11)} $total")
        }
      }
    }

  addSubCommand(split)
}

object Main {
  def main(args: Array[String]) = new Cert(args).runCommand()
}
