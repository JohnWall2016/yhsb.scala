import yhsb.base.command.Command
import yhsb.base.command.Subcommand
import yhsb.base.command.InputFile
import yhsb.base.command.RowRange

import yhsb.base.excel.Excel._
import yhsb.base.io.Path._

import yhsb.cjb.net.protocol.Division.GroupOps
import java.nio.file.Files

object Main {
  def main(args: Array[String]) = new Lookback(args).runCommand()
}

class Lookback(args: collection.Seq[String]) extends Command(args) {

  banner("回头看数据处理程序")

  val up =
    new Subcommand("dyryhcb") with InputFile with RowRange {
      descr("生成待遇人员核查表")

      val outputDir = """D:\数据核查\待遇核查回头看"""

      val template = outputDir / """待遇人员入户核查表.xlsx"""

      def execute(): Unit = {
        val destDir = outputDir / "待遇发放人员入户核查表"

        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        println("生成分组映射表")
        val map = (for (index <- (startRow() - 1) until endRow())
          yield {
            (sheet.getRow(index)("E").value, index)
          }).groupByDwAndCsName()

        println("生成待遇发放人员入户核查表")
        if (Files.exists(destDir)) {
          Files.move(destDir, s"${destDir.toString}.orig")
        }
        Files.createDirectory(destDir)

        for ((dw, csMap) <- map) {
          println(s"$dw:")
          Files.createDirectory(destDir / dw)

          for ((cs, indexes) <- csMap) {
            println(s"  $cs: ${indexes.mkString(",")}")
            Files.createDirectory(destDir / dw / cs)

            val outWorkbook = Excel.load(template)
            val outSheet = outWorkbook.getSheetAt(0)
            val startRow = 7
            var currentRow = startRow

            indexes.foreach { rowIndex =>
              val index = currentRow - startRow + 1
              val inRow = sheet.getRow(rowIndex)

              println(s"    $index ${inRow("C").value} ${inRow("D").value}")

              val outRow = outSheet.getOrCopyRow(currentRow, startRow)
              currentRow += 1
              outRow("A").value = index
              outRow("B").value = inRow("B").value
              outRow("C").value = inRow("D").value
              outRow("D").value = inRow("C").value
              outRow("E").value = inRow("E").value
              outRow("F").value = "城乡居保"
              outRow("G").value = inRow("I").value
              outRow("H").value = inRow("J").value
              outRow("I").value = inRow("F").value
              outRow("J").value = inRow("H").value
              outRow("K").value = inRow("G").value
            }

            outWorkbook.save(destDir / dw / cs / s"${cs}待遇人员入户核查表.xlsx")
          }
        }
      }
    }
}
