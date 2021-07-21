import yhsb.base.command.Command
import yhsb.base.command.Subcommand
import yhsb.base.command.InputFile
import yhsb.base.command.RowRange

import yhsb.base.excel.Excel._
import yhsb.base.io.Path._

import yhsb.base.zip

import yhsb.cjb.net.protocol.Division.GroupOps

import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import java.nio.file.Path

import yhsb.base.datetime.Formatter
import yhsb.base.command.InputDir
import yhsb.cjb.db.Lookback2021

import yhsb.base.db.Context.JdbcContextOps

object Main {
  def main(args: Array[String]) = new Lookback(args).runCommand()
}

class Lookback(args: collection.Seq[String]) extends Command(args) {

  banner("回头看数据处理程序")

  val zipSubDir = new Subcommand("zip") with InputDir {
    descr("打包子目录")

    val fileNameTemplate = trailArg[String](
      descr = "生成文件名模板"
    )

    def execute(): Unit = {
      new File(inputDir()).listFiles.foreach { f =>
        if (f.isDirectory()) {
          zip.packDir(
            f,
            inputDir() / s"${fileNameTemplate()}(${f.getName()})${Formatter.formatDate()}.zip"
          )
        }
      }
    }
  }

  val retiredTables = new Subcommand("dyhcb") with InputFile with RowRange {
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

      var total = 0
      for ((dw, csMap) <- map) {
        val subTotal = csMap.foldLeft(0)(_ + _._2.size)
        total += subTotal
        println(s"\n$dw: ${subTotal}")
        Files.createDirectory(destDir / dw)

        for ((cs, indexes) <- csMap) {
          println(s"  $cs: ${indexes.size}")
          Files.createDirectory(destDir / dw / cs)

          val outWorkbook = Excel.load(template)
          val outSheet = outWorkbook.getSheetAt(0)
          val startRow = 7
          var currentRow = startRow

          indexes.foreach { rowIndex =>
            val index = currentRow - startRow + 1
            val inRow = sheet.getRow(rowIndex)

            //println(s"    $index ${inRow("C").value} ${inRow("D").value}")

            val outRow = outSheet.getOrCopyRow(currentRow, startRow)
            currentRow += 1
            outRow("A").value = index
            //outRow("B").value = inRow("B").value
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
      println(s"\n共计: ${total}")
    }
  }

  val loadCardsData = new Subcommand("ldcards") with InputDir {
    descr("导入社保卡数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(cardData.delete)
        println("结束清除数据")
      }

      new File(inputDir()).listFiles.foreach { f =>
        println(s"导入 $f")
        Lookback2021.loadExcel(
          cardData.quoted,
          f.toString(),
          2,
          fields = Seq("C", "B", "D", "I", "J", "社保卡", "", "", "")
        )
      }
    }
  }

  val loadJbData = new Subcommand("ldjb") with InputDir {
    descr("导入居保数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(jbData.delete)
        println("结束清除数据")
      }

      new File(inputDir()).listFiles.foreach { f =>
        println(s"导入 $f")
        Lookback2021.loadExcel(
          jbData.quoted,
          f.toString(),
          2,
          fields = Seq("F", "D", "A", "", "", "居保", "", "", "")
        )
      }
    }
  }

  val loadQmcbData = new Subcommand("ldqmcb") with InputFile {
    descr("导入全民参保数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(qmcbData.delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      Lookback2021.loadExcel(
        qmcbData.quoted,
        inputFile(),
        3,
        fields = Seq("C", "B", "D", "", "", "全民参保", "K", "L", "M")
      )
    }
  }

  addSubCommand(retiredTables)
  addSubCommand(zipSubDir)
  addSubCommand(loadCardsData)
  addSubCommand(loadJbData)
  addSubCommand(loadQmcbData)
}
