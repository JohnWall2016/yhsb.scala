package yhsb.app.cjb.cert

import yhsb.base.command._
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._
import yhsb.base.text.Strings.StringOps
import yhsb.cjb.net.protocol.Xzqh

import scala.collection.mutable

import org.apache.poi.ss.usermodel.Sheet
import java.nio.file.Files
import java.nio.file.Path

class Cert(args: Seq[String]) extends Command(args) {
  banner("待遇认证数据统计和表格生成程序")

  val split =
    new Subcommand("split") with InputFile with RowRange with OutputDir {

      def xzqhRow() = "A"

      val template = """D:\待遇认证\2020年\城乡居民基本养老保险待遇领取人员资格认证表（表二）.xls"""

      case class Group(
          var total: Int,
          data: mutable.Map[String, mutable.ListBuffer[Int]]
      )

      def generateGroups(
          sheet: Sheet,
          beginRow: Int,
          endRow: Int
      ): collection.Map[String, Group] = {
        val map = mutable.Map[String, Group]()

        for (index <- (beginRow - 1) until endRow) {
          val xzqh = sheet.getRow(index).getCell(xzqhRow()).value
          val dwAndCs = Xzqh.getDwAndCsName(xzqh)
          if (dwAndCs.isEmpty) throw new Exception(s"未匹配的行政区划: $xzqh")
          val (dw, cs) = dwAndCs.get
          if (!map.contains(dw)) {
            map(dw) = Group(0, mutable.Map())
          }
          map(dw).total += 1
          if (!map(dw).data.contains(cs)) {
            map(dw).data(cs) = mutable.ListBuffer(index)
          } else {
            map(dw).data(cs).append(index)
          }
        }
        map
      }

      override def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        def createDir(path: Path) = {
          if (!Files.exists(path)) {
            Files.createDirectory(path)
          }
        }

        println(s"生成数据分组")
        val groups = generateGroups(sheet, startRow(), endRow())

        var total = 0
        println(s"导出数据到: ${outputDir()}")
        createDir(Path.of(outputDir()))
        for ((xzj, group) <- groups) {
          println(s"\n${(xzj + ":").padRight(11)}   ${group.total}")
          total += group.total
          createDir(Path.of(outputDir(), xzj))

          for ((cs, indexes) <- groups(xzj).data) {
            println(s"  ${(cs + ":").padRight(11)} ${indexes.size}")
            val outWorkbook = Excel.load(template)
            val outSheet = outWorkbook.getSheetAt(0)

            outSheet.getCell("C2").setCellValue(s"$xzj$cs")
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

            outWorkbook.save(Path.of(outputDir(), xzj, s"${cs}.xls"))
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
