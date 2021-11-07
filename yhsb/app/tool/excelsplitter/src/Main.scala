package yhsb.app.tool

import yhsb.base.command.Command
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._

import scala.collection.mutable

object Main {
  def main(args: Array[String]) = new ExcelSplitter(args).runCommand()
}

class ExcelSplitter(args: collection.Seq[String]) extends Command(args) {
  banner("Excel分组程序")

  val inputFile = trailArg[String](descr = "输入文件路径")

  val startRow = trailArg[Int](descr = "开始行, 从1开始")

  val endRow = trailArg[Int](descr = "结束行, 包含在内")

  val colName = trailArg[String](descr = "用于分组的列")

  val templateFile = trailArg[String](descr = "模板文件路径")

  val startIndex = trailArg[Int](descr = "模板开始写入行, 从1开始")

  val fromCols = trailArg[String](descr = "选取写入的列, 如: \"A,B,C\"")

  val toCols = trailArg[String](descr = "写入模板的列, 如: \"B,C,D\",\"B,C,D,@A\"")

  val outputFormat = trailArg[String](descr = "输出文件路径格式, 如: D:\\路径1\\$1导出.xls")

  override def execute(): Unit = {
    val inWorkbook = Excel.load(inputFile())
    val inSheet = inWorkbook.getSheetAt(0)

    println(s"生成数据分组")
    val map = mutable.LinkedHashMap[String, mutable.ListBuffer[Int]]()
    for (index <- (startRow() - 1) until endRow()) {
      map
        .getOrElseUpdate(
          inSheet.getRow(index)(colName()).value.trim(),
          mutable.ListBuffer()
        )
        .addOne(index)
    }

    println(s"生成excel文件")
    for ((name, indexes) <- map) {
      println(s"$name: ${indexes.size}")

      val outWorkbook = Excel.load(templateFile())
      val outSheet = outWorkbook.getSheetAt(0)
      var currentIndex, fromIndex = startIndex()

      val fromColNames = fromCols().split(""".*,.*""")
      val (indexColNames, toColNames) =
        toCols().split(""".*,.*""").partition(_.startsWith("@"))
      val indexColName = indexColNames.headOption.map(_.drop(1))
      val colCount = math.min(fromColNames.size, toColNames.size)

      for (index <- indexes) {
        val inRow = inSheet.getRow(index)
        val outRow = outSheet.getOrCopyRow(currentIndex, fromIndex)

        if (indexColName.isDefined) {
          outRow.getCell(indexColName.get).value = currentIndex - fromIndex + 1
        }

        (0 until colCount).foreach { i =>
          outRow.getCell(toColNames(i)).value =
            inRow.getCell(fromColNames(i)).value
        }

        currentIndex += 1
      }
      
      outWorkbook.save(outputFormat().replaceAll("""\$1""", name))
    }
  }
}
