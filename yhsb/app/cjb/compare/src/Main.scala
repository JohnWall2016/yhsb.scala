import io.getquill.Query

import java.nio.file.Files

import org.rogach.scallop.ScallopConf
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.Sheet

import yhsb.base.command.Command
import yhsb.base.text.String._
import yhsb.base.excel.Excel._
import yhsb.base.command.Subcommand
import yhsb.base.db.Context.JdbcContextOps
import yhsb.base.datetime.Formatter
import yhsb.base.io.Path._

import yhsb.cjb.db.CompareData2021
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.PersonInfoQuery
import yhsb.cjb.net.protocol.JBState
import yhsb.cjb.net.protocol.JBKind
import yhsb.cjb.db.Jgsbdup
import yhsb.cjb.net.protocol.Division

object Main {
  def main(args: Array[String]): Unit = new Compare(args).runCommand()
}

class Compare(args: collection.Seq[String]) extends Command(args) {
  banner("城居数据比对程序")

  addSubCommand(new ExportJBData)
  addSubCommand(new ImportJBData)
  addSubCommand(new UpdateJBState)
  addSubCommand(new ImportJgsbData)
  addSubCommand(new ExportJgsbDupData)
}

class ExportJBData extends Subcommand("dcjb") {
  descr("导出居保参保数据")

  val outputDir = """D:\数据核查\参保人员明细表"""

  override def execute(): Unit = {
    val joinedDates = List(
      "" -> "2010-01-26",
      "2010-01-27" -> "2011-12-31",
      "2012-01-01" -> ""
    )

    for (((start, end), index) <- joinedDates.zipWithIndex) {
      println(s"开始导出参保时间段: $start -> $end")

      val exportFile = Files.createTempFile("yhsb", ".xls").toString
      Session.use() {
        _.exportAllTo(
          PersonInfoQuery(start, end),
          PersonInfoQuery.columnMap
        )(
          exportFile
        )
      }

      val workbook = Excel.load(exportFile)
      val sheet = workbook.getSheetAt(0)
      sheet.setColumnWidth(0, 35 * 256)
      sheet.setColumnWidth(2, 12 * 256)
      sheet.setColumnWidth(3, 8 * 256)
      sheet.setColumnWidth(4, 20 * 256)
      sheet.setColumnWidth(5, 20 * 256)

      workbook.saveAfter(
        outputDir / s"居保参保人员明细表${Formatter.formatDate()}${('A' + index).toChar}.xls"
      ) { path =>
        println(s"保存: $path")
      }
    }

    println("结束数据导出")
  }
}

class ImportJBData extends Subcommand("drjb") {
  descr("导入居保参保数据")

  val excel = trailArg[String](descr = "excel表格文件路径")
  val startRow = trailArg[Int](descr = "开始行, 从1开始")
  val endRow = trailArg[Int](descr = "结束行, 包含在内")
  val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

  override def execute(): Unit = {
    import CompareData2021._

    if (clear()) {
      println("开始清除数据: 居保参保人员明细表")
      run(jbrymxData.delete)
      println("结束清除数据: 居保参保人员明细表")
    }

    println("开始导入居保参保人员明细表")

    CompareData2021.loadExcel(
      jbrymxData.quoted,
      excel(),
      startRow(),
      endRow(),
      Seq("F", "A", "B", "D", "G", "H", "J", "L", "M", "R", "", ""),
      printSql = true
    )

    println("结束导入居保参保人员明细表")
  }
}

class UpdateJBState extends Subcommand("jbzt") {
  descr("更新居保参保状态、参保身份")

  override def execute(): Unit = {
    import CompareData2021._

    println(s"开始更新居保状态")

    transaction {
      val peopleTable = "jbrymx"
      for (((cbState, jfState), jbState) <- JBState.jbStateMap) {
        val sql =
          s"""update $peopleTable
             |   set ${peopleTable}.jbzt='$jbState'
             | where ${peopleTable}.cbzt='$cbState'
             |   and ${peopleTable}.jfzt='$jfState'""".stripMargin
        CompareData2021.execute(sql, true)
      }
      for ((code, name) <- JBKind) {
        val sql =
          s"""update $peopleTable
             |   set ${peopleTable}.cbsf='$name'
             | where ${peopleTable}.cbsf='$code'""".stripMargin
        CompareData2021.execute(sql, true)
      }
    }

    println(s"结束更新居保状态")
  }
}

class ImportJgsbData extends Subcommand("drjg") {
  descr("导入机关事保参保数据")

  val excel = trailArg[String](descr = "excel表格文件路径")
  val sheetIndex = trailArg[Int](descr = "数据表序号, 从1开始")
  val startRow = trailArg[Int](descr = "开始行, 从1开始")
  val endRow = trailArg[Int](descr = "结束行, 包含在内")

  val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

  override def execute(): Unit = {
    import CompareData2021._

    if (clear()) {
      println("开始清除数据: 机关事保参保人员明细表")
      run(jgsbrymxData.delete)
      println("结束清除数据: 机关事保参保人员明细表")
    }

    println("开始导入机关事保参保人员明细表")

    val workbook = Excel.load(excel())
    val sheetName = workbook.getSheetAt(sheetIndex() - 1).getSheetName()

    CompareData2021.loadExcel(
      jgsbrymxData.quoted,
      workbook,
      startRow(),
      endRow(),
      Seq("B", "C", sheetName, ""),
      printSql = true,
      tableIndex = sheetIndex() - 1
    )

    println("结束导入机关事保参保人员明细表")
  }
}

class ExportJgsbDupData extends Subcommand("jgcf") {
  descr("导出机关事保重复参保数据")

  val path = """D:\数据核查"""
  val template = path / "重复参保数据底册模板.xlsx"

  override def execute(): Unit = {
    import CompareData2021._

    println("开始导出机关事保重复参保数据")

    val data: List[Jgsbdup] = run(JgsbdupData)

    val workbook = Excel.load(template)
    val sheet = workbook.getSheetAt(0)

    val startRow = 2
    var currentRow = startRow

    data.foreach { it =>
      val index = currentRow - startRow + 1

      println(s"$index ${it.idCard} ${it.name.getOrElse("")}")

      val row = sheet.getOrCopyRow(currentRow, startRow)

      val (dw, cs) =
        Division.getDwAndCsName(it.division.getOrElse("")).getOrElse(("", ""))

      row("A").value = index
      row("B").value = dw
      row("C").value = cs
      row("D").value = it.division
      row("E").value = it.name
      row("F").value = it.idCard
      row("G").value = it.birthDay
      row("H").value = it.jbState
      row("I").value = it.jbKind
      row("J").value = it.jgsbName
      row("K").value = it.jgsbArea

      currentRow += 1
    }

    workbook.saveAfter(path / s"机关事保重复参保数据${Formatter.formatDate()}.xlsx") { p =>
      println(s"保存: $p")
    }

    println("结束导出机关事保重复参保数据")
  }
}
