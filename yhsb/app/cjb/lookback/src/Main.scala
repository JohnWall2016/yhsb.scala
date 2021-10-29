package yhsb.app.cjb

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

import io.getquill.Ord
import io.getquill.Query
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.FormulaEvaluator
import yhsb.base.command.Command
import yhsb.base.command.InputDir
import yhsb.base.command.InputFile
import yhsb.base.command.RowRange
import yhsb.base.command.Subcommand
import yhsb.base.datetime.Formatter
import yhsb.base.datetime.YearMonth
import yhsb.base.db.Context.JdbcContextOps
import yhsb.base.excel.Excel._
import yhsb.base.io.Path._
import yhsb.base.io.File.listFiles
import yhsb.base.text.String._
import yhsb.base.zip
import yhsb.cjb.db.lookback._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CBState
import yhsb.cjb.net.protocol.Division
import yhsb.cjb.net.protocol.Division.GroupOps
import yhsb.cjb.net.protocol.LookBackTable1Audit
import yhsb.cjb.net.protocol.LookBackTable2Audit
import yhsb.cjb.net.protocol.LookBackTable2Cancel
import yhsb.cjb.net.protocol.PauseReason
import yhsb.cjb.net.protocol.PayStopReason
import yhsb.cjb.net.protocol.PaymentTerminateQuery
import yhsb.cjb.net.protocol.PersonInfoPaylistQuery
import yhsb.cjb.net.protocol.PersonInfoQuery
import yhsb.cjb.net.protocol.RefundQuery
import yhsb.cjb.net.protocol.RetiredPersonPauseQuery
import yhsb.cjb.net.protocol.RetiredPersonStopAuditQuery
import yhsb.base.command.OutputDir
import yhsb.cjb.net.protocol.LookBackTable1Query
import yhsb.cjb.net.protocol.LookBackTable2Query

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

    val issueName = trailArg[String]("下发数据名称")

    def execute(): Unit = {
      val destDir = outputDir / "待遇发放人员入户核查表" / issueName()

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
        println(s"\r\n$dw: ${subTotal}")
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
      println(s"\r\n共计: ${total}")
    }
  }

  val retiredTables2 = new Subcommand("dyhcb2") with InputFile with RowRange {
    descr("生成待遇人员核查表")

    val outputDir = """D:\数据核查\回头看数据复核"""

    val template = outputDir / """待遇人员入户核查表.xlsx"""

    def execute(): Unit = {
      val destDir = outputDir / "异常情况待遇人员入户核查表"

      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      println("生成分组映射表")
      val map = mutable.LinkedHashMap[String, mutable.ListBuffer[Int]]()
      for (index <- (startRow() - 1) until endRow()) {
        val list = map.getOrElseUpdate(
          sheet.getRow(index)("A").value,
          mutable.ListBuffer()
        )
        list.addOne(index)
      }

      println("生成待遇发放人员入户核查表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      var total = 0
      for ((dw, indexes) <- map) {
        val subTotal = indexes.size
        total += subTotal
        println(s"\r\n$dw: ${subTotal}")

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
          outRow("C").value = inRow("E").value
          outRow("D").value = inRow("D").value
          outRow("E").value = inRow("C").value
          outRow("F").value = "城乡居保"
          outRow("G").value = inRow("V").value
          outRow("H").value = inRow("W").value
          outRow("I").value = inRow("G").value
          outRow("J").value = inRow("I").value
          outRow("K").value = inRow("H").value
          val phones = inRow("S").value.split(",").foldLeft("") { (s, p) =>
            if (p != "") {
              if (s != "") s + "," + p
              else p
            } else {
              ""
            }
          }
          val deathTime = {
            val kind = inRow("T").value
            if (kind != "") s"$kind(${inRow("U").value})"
            else ""
          }
          outRow("X").value = {
            var r = ""
            if (phones != "") r = s"联系方式:$phones"
            if (deathTime != "") {
              if (r != "") r += "\n"
              r += s"疑似死亡:$deathTime"
            }
            r
          }
        }

        outWorkbook.save(destDir / s"${dw}异常情况待遇人员入户核查表.xlsx")
      }

      println(s"\r\n共计: ${total}")
    }
  }

  val loadCardsData = new Subcommand("ldcards") with InputDir {
    descr("导入社保卡数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    val reserve1 = opt[String](descr = "reserve1字段设置内容", default = Some(""))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

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
          fields = Seq("C", "B", "D", "I", "J", "社保卡", reserve1(), "", "")
        )
      }
    }
  }

  val loadJbData = new Subcommand("ldjb") with InputDir {
    descr("导入居保数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

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

      println("更新社保卡信息")
      Lookback2021.execute(
        s"update ${jbData.quoted.name} as a, cards_data as b " +
          "set a.bank_name=b.bank_name, a.card_number=b.card_number " +
          "where a.idcard=b.idcard;",
        true
      )
    }
  }

  val loadQmcbData = new Subcommand("ldqmcb") with InputFile {
    descr("导入全民参保数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

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

      println("更新社保卡信息")
      Lookback2021.execute(
        s"update ${qmcbData.quoted.name} as a, cards_data as b " +
          "set a.bank_name=b.bank_name, a.card_number=b.card_number " +
          "where a.idcard=b.idcard;",
        true
      )
    }
  }

  val loadRetiredData = new Subcommand("ldretire") with InputFile {
    descr("导入待遇人员数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(retiredData.delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      Lookback2021.loadExcel(
        retiredData.quoted,
        inputFile(),
        2,
        fields = Seq("B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L")
      )
    }
  }

  val unionAllData = new Subcommand("union") {
    descr("合并居保、社保卡、全名参保数据数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(unionData.delete)
        println("结束清除数据")
      }

      def unionTable(tableName: String) = {
        println(s"开始合并 $tableName")
        val unionTable = unionData.quoted.name
        Lookback2021.execute(
          s"insert into $unionTable " +
            s"select * from $tableName " +
            s"on duplicate key update ${unionTable}.idcard=${unionTable}.idcard;",
          true
        )
        println(s"结束合并 $tableName")
      }

      unionTable(jbData.quoted.name)
      unionTable(cardData.quoted.name)
      unionTable(qmcbData.quoted.name)
    }
  }

  val loadPoliceData = new Subcommand("ldpolice") with InputFile {
    descr("导入公安数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      import scala.jdk.CollectionConverters._

      if (clear()) {
        println("开始清除数据")
        run(policeData.delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      val workbook = Excel.load(inputFile())
      for (sheet <- workbook.sheetIterator().asScala) {
        println(s"  导入表: ${sheet.getSheetName()}")
        Lookback2021.loadSheet(
          policeData.quoted,
          sheet,
          2,
          fields = Seq("B", "A", "C", "", "", "公安", "", "", "")
        )
      }
    }
  }

  val exportPoliceData = new Subcommand("expolice") with InputFile {
    descr("导出公安数据")

    val condition = trailArg[String](descr = "查询条件")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      import scala.jdk.CollectionConverters._

      val items: List[Table1] = run {
        quote {
          infix"$policeData #${condition()}".as[Query[Table1]]
        }
      }

      println(items.size)

      Excel.export[Table1](
        items,
        inputFile(),
        (index, row, item) => {
          row.getOrCreateCell("A").value = item.name
          row.getOrCreateCell("B").value = item.idCard
          row.getOrCreateCell("C").value = item.address
          row.getOrCreateCell("D").value = "430302"
        },
        (row) => {
          row.getOrCreateCell("A").value = "姓名"
          row.getOrCreateCell("B").value = "身份证号码"
          row.getOrCreateCell("C").value = "地址"
          row.getOrCreateCell("D").value = "行政区划"
        },
        60000
      )
    }
  }

  val deleteRetired = new Subcommand("delretire") {
    descr("删除合并数据中的待遇人员")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      val unionTable = unionData.quoted.name
      val retiredTable = retiredData.quoted.name
      Lookback2021.execute(
        s"delete from $unionTable " +
          s"where idcard in (select idcard from $retiredTable);",
        true
      )
    }
  }

  val updateDwAndCs = new Subcommand("updwcs") {
    descr("更新乡镇(街道)、村(社区)信息")

    case class XzqhInfo(
        matchDwName: String,
        matchCsName: String,
        dwName: String,
        csName: String
    )
    val xzqhExcel = """D:\数据核查\待遇核查回头看\行政区划对照表.xls"""

    val regExps = Seq[Regex](
      "湘潭市雨湖区(((.*?)乡)((.*?)社区)).*".r,
      "湘潭市雨湖区(((.*?)乡)((.*?)村)).*".r,
      "湘潭市雨湖区(((.*?)乡)((.*?政府机关))).*".r,
      "湘潭市雨湖区(((.*?)街道)办事处((.*?)社区)).*".r,
      "湘潭市雨湖区(((.*?)街道)办事处((.*?政府机关))).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?)社区)).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?)居委会)).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?)村)).*".r,
      "湘潭市雨湖区(((.*?)街道)办事处((.*?)村)).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?政府机关))).*".r
    )

    def loadXzqhTable(): collection.Seq[XzqhInfo] = {
      val workbook = Excel.load(xzqhExcel)
      val sheet = workbook.getSheetAt(0)

      val list = ListBuffer[XzqhInfo]()
      for (row <- sheet.rowIterator(1)) {
        val division = row("B").value
        var dwName = row("C").value
        var csName = row("D").value

        for (r <- regExps) {
          division match {
            case r(_, dw, dwMatch, cs, csMatch) =>
              list.addOne(
                XzqhInfo(
                  dwMatch,
                  csMatch,
                  if (dwName.isEmpty()) dw else dwName,
                  if (csName.isEmpty()) cs else csName
                )
              )
            case _ =>
          }
        }
      }
      list
    }

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val list = loadXzqhTable()

      val unionTable = unionData.quoted.name

      for (info <- list) {
        Lookback2021.execute(
          s"update $unionTable " +
            s"set reserve1='${info.dwName}', reserve2='${info.csName}' " +
            s"where address like '%${info.matchDwName}%${info.matchCsName}%';",
          true
        )
      }
    }
  }

  val cbckTables = new Subcommand("cbck") {
    descr("生成参保和持卡情况核查表")

    val outputDir = """D:\数据核查\待遇核查回头看"""

    val template = outputDir / """参保与持卡情况表.xlsx"""

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val destDir = outputDir / "参保与持卡情况表" / "第一批"

      println("生成参保和持卡情况核查表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val groups = run(
        unionData
          .filter(d => d.reserve1 != "" && d.reserve2 != "")
          .sortBy(d => (d.reserve1, d.reserve2))
          .groupBy(d => (d.reserve1, d.reserve2))
          .map { case (group, items) =>
            (group._1, group._2, items.size)
          }
      )

      var dwName = ""
      var total = 0
      for ((dw, cs, size) <- groups /*.take(4)*/ ) {
        if (dwName != dw) {
          val subTotal =
            groups.foldLeft(0)((n, e) => if (e._1 == dw) n + e._3.toInt else n)
          total += subTotal
          println(s"\r\n$dw: $subTotal")
          Files.createDirectory(destDir / dw)
          dwName = dw
        }
        println(s"  $cs: $size")
        Files.createDirectory(destDir / dw / cs)

        val items: List[Table1] = run {
          quote(
            infix"${unionData.filter(d => d.reserve1 == lift(dw) && d.reserve2 == lift(cs))} ORDER BY CONVERT( name USING gbk )"
              .as[Query[Table1]]
          )
        }

        val workbook = Excel.load(template)
        val sheet = workbook.getSheetAt(0)

        val startRow = 6
        var currentRow = startRow

        items.foreach { item =>
          val index = currentRow - startRow + 1
          val row = sheet.getOrCopyRow(currentRow, startRow)
          currentRow += 1
          row("A").value = index
          row("C").value = item.name
          row("D").value = item.idCard
          row("E").value = item.address
          if (item.dataType == "居保") {
            row("F").value = "√"
            row("H").value = "√"
          }
          if (item.cardNumber != "") {
            row("N").value = "√"
            row("P").value = item.cardNumber
            row("Q").value = item.bankName
          }
        }

        workbook.save(destDir / dw / cs / s"${cs}参保和持卡情况核查表.xlsx")
      }
      println(s"\r\n共计: ${total}")
    }
  }

  val qbcbckTables = new Subcommand("qbcbck") with InputFile with RowRange {
    descr("生成职保参保和持卡情况核查表")

    val outputDir = """D:\数据核查\待遇核查回头看"""

    val template = outputDir / """参保与持卡情况表.xlsx"""

    def execute(): Unit = {
      val destDir = outputDir / "职保参保与持卡情况表" / "第二批"

      println("生成参保和持卡情况核查表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      val map = mutable
        .LinkedHashMap[String, mutable.Map[String, mutable.ListBuffer[Int]]]()

      for (index <- (startRow() - 1) until endRow()) {
        val row = sheet.getRow(index)
        val countryName = row("S").value.trim()
        var villageName = row("T").value.trim()
        villageName = if (villageName == "") "未分村社区" else villageName
        val subMap = map.getOrElseUpdate(countryName, mutable.LinkedHashMap())
        val list = subMap.getOrElseUpdate(villageName, mutable.ListBuffer())
        list.addOne(index)
      }

      var total = 0
      for ((dw, subMap) <- map) {
        val subTotal = subMap.values.foldLeft(0)((n, l) => n + l.size)
        total += subTotal
        println(s"\r\n$dw: $subTotal")
        Files.createDirectory(destDir / dw)
        for ((cs, list) <- subMap) {
          println(s"  $cs: ${list.size}")
          Files.createDirectory(destDir / dw / cs)

          val outWorkbook = Excel.load(template)
          val outSheet = outWorkbook.getSheetAt(0)

          val startRow = 6
          var currentRow = startRow

          list.foreach { rowIndex =>
            val index = currentRow - startRow + 1
            val sourceRow = sheet.getRow(rowIndex)
            val outRow = outSheet.getOrCopyRow(currentRow, startRow)
            currentRow += 1
            outRow("A").value = index
            outRow("C").value = sourceRow("F").value
            outRow("D").value = sourceRow("G").value
            outRow("E").value = sourceRow("H").value
            outRow("F").value = "√"
            outRow("I").value = "√"

            val cardNumber = sourceRow("L").value.trim()
            if (cardNumber != "") {
              val cardType = sourceRow("K").value.trim()
              if (cardType == "国家社保卡（新卡）") {
                outRow("N").value = "√"
              } else if (cardType == "湖南省社保卡（老卡）") {
                outRow("O").value = "√"
              }
              outRow("P").value = cardNumber
              outRow("R").value = sourceRow("M").value
            }
            outRow("U").value = sourceRow
              .getValues("N", "O", "P")
              .foldLeft("")((p, v) => {
                val v2 = v.trim()
                if (v2 != "") {
                  if (p != "") p + "/" + v2
                  else v2
                } else {
                  p
                }
              })
          }

          outWorkbook.save(destDir / dw / cs / s"${cs}参保和持卡情况核查表.xlsx")
        }
      }
      println(s"\r\n共计: ${total}")
    }
  }

  val generateAddressTable = new Subcommand("gencomp") {
    descr("自动生成部分户籍地址到行政区划匹配表")

    case class XzqhInfo(
        matchDwName: String,
        matchCsName: String,
        dwName: String,
        csName: String
    )
    val xzqhExcel = """D:\数据核查\待遇核查回头看\行政区划对照表.xls"""

    val regExps = Seq[Regex](
      "湘潭市雨湖区(((.*?)乡)((.*?)社区)).*".r,
      "湘潭市雨湖区(((.*?)乡)((.*?)村)).*".r,
      "湘潭市雨湖区(((.*?)乡)((.*?政府机关))).*".r,
      "湘潭市雨湖区(((.*?)街道)办事处((.*?)社区)).*".r,
      "湘潭市雨湖区(((.*?)街道)办事处((.*?政府机关))).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?)社区)).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?)居委会)).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?)村)).*".r,
      "湘潭市雨湖区(((.*?)街道)办事处((.*?)村)).*".r,
      "湘潭市雨湖区(((.*?)镇)((.*?政府机关))).*".r
    )

    def loadXzqhTable(): collection.Seq[XzqhInfo] = {
      val workbook = Excel.load(xzqhExcel)
      val sheet = workbook.getSheetAt(0)

      val list = ListBuffer[XzqhInfo]()
      for (row <- sheet.rowIterator(1)) {
        val division = row("B").value
        var dwName = row("C").value
        var csName = row("D").value

        for (r <- regExps) {
          division match {
            case r(_, dw, dwMatch, cs, csMatch) =>
              list.addOne(
                XzqhInfo(
                  dwMatch,
                  csMatch,
                  if (dwName.isEmpty()) dw else dwName,
                  if (csName.isEmpty()) cs else csName
                )
              )
            case _ =>
          }
        }
      }
      list
    }

    val addressTable = """D:\数据核查\待遇核查回头看\公安数据\户籍地址到行政区划匹配表.xlsx"""

    def execute(): Unit = {
      val list = loadXzqhTable()
      val workbook = Excel.load(addressTable)
      val sheet = workbook.getSheetAt(0)
      var rowIndex = 1
      for (info <- list) {
        val row = sheet.createRow(rowIndex)
        rowIndex += 1
        row.getOrCreateCell("A").value = rowIndex - 1
        row.getOrCreateCell("B").value = info.matchDwName
        row.getOrCreateCell("C").value = info.matchCsName
        row.getOrCreateCell("D").value = ""
        row.getOrCreateCell("E").value = info.dwName
        row.getOrCreateCell("F").value = info.csName
        row.getOrCreateCell("G").value = ""
        row.getOrCreateCell("H").value = ""
      }
      workbook.save(addressTable.insertBeforeLast("up"))
    }
  }

  val unionTable1Data = new Subcommand("untable1") {
    descr("合并公安、居保参保数据数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(table1Data.delete)
        println("结束清除数据")
      }

      def unionTable(tableName: String, cond: String = "") = {
        println(s"开始合并 $tableName")
        val unionTable = table1Data.quoted.name
        Lookback2021.execute(
          s"insert into $unionTable " +
            s"(select * from $tableName $cond) " +
            s"on duplicate key update ${unionTable}.idcard=${unionTable}.idcard;",
          true
        )
        println(s"结束合并 $tableName")
      }

      unionTable(
        policeData.quoted.name,
        "where address not like '%响水乡%' and address not like '%和平街道%' and address not like '%雨湖区红旗社区%' and address not like '%九华%' and address not like '%雨湖区响水%' and address not like '%科大%' and address not like '%石马头%' and address not like '%雨湖区石码头%' and address not like '%雨湖区合山社区%' and address not like '%雨湖区吉利社区%' and address not like '雨湖区将军渡社区' and address not like '%雨湖区杉山社区%' ORDER BY CONVERT( address USING gbk )"
      )
      unionTable(jbData.quoted.name)
    }
  }

  val mapTable1Data = new Subcommand("mptable1") with InputFile with RowRange {
    descr("将附表1数据分到乡镇(街道)、村(社区)")

    case class AddressMapInfo(
        matchField1: String,
        matchField2: String,
        matchField3: String,
        countryName: String,
        villageName: String,
        groupName: String
    )

    def loadAddressTable(): Seq[AddressMapInfo] = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      for (index <- (startRow() - 1) until endRow()) yield {
        val row = sheet.getRow(index)
        AddressMapInfo(
          row("B").value,
          row("C").value,
          row("D").value,
          row("E").value,
          row("F").value,
          row("G").value
        )
      }
    }

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val list = loadAddressTable()

      val table1 = table1Data.quoted.name

      for (info <- list) {
        Lookback2021.execute(
          s"update $table1 " +
            s"set reserve1='${info.countryName}', reserve2='${info.villageName}', reserve3='${info.groupName}' " +
            s"where reserve1='' and address like '%${info.matchField1}%${info.matchField2}%${info.matchField3}%';",
          true
        )
      }
    }
  }

  val exportTable1Data = new Subcommand("extable1") with InputFile {
    descr("导出附表1数据")

    val condition = trailArg[String](descr = "查询条件")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      import scala.jdk.CollectionConverters._

      val items: List[Table1] = run {
        quote {
          infix"$table1Data #${condition()}".as[Query[Table1]]
        }
      }

      println(items.size)

      Excel.export[Table1](
        items,
        inputFile(),
        (index, row, item) => {
          row.getOrCreateCell("A").value = item.name
          row.getOrCreateCell("B").value = item.idCard
          row.getOrCreateCell("C").value = item.address
          row.getOrCreateCell("D").value = "430302"
          row.getOrCreateCell("E").value = item.reserve1
          row.getOrCreateCell("F").value = item.reserve2
        },
        (row) => {
          row.getOrCreateCell("A").value = "姓名"
          row.getOrCreateCell("B").value = "身份证号码"
          row.getOrCreateCell("C").value = "地址"
          row.getOrCreateCell("D").value = "行政区划"
          row.getOrCreateCell("E").value = "乡镇（街道）"
          row.getOrCreateCell("F").value = "村（社区）"
        },
        60000
      )
    }
  }

  val insertTable1Data = new Subcommand("intable1")
    with InputFile
    with RowRange {
    descr("新增附表1数据")

    val batch = trailArg[String](descr = "注明批次")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      transaction {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)
        for (index <- (startRow() - 1) until endRow()) {
          val row = sheet.getRow(index)
          val Seq(name, idCard, address, countryName, villageName) =
            row.getValues("A", "B", "C", "E", "F")
          println(s"${index + 1} $idCard $name")
          val data: List[Table1] =
            run(table1Data.filter(_.idCard == lift(idCard)))
          if (data.isEmpty) {
            run {
              table1Data.insert(
                _.name -> lift(name),
                _.idCard -> lift(idCard),
                _.address -> lift(address),
                _.reserve1 -> lift(countryName.trim()),
                _.reserve2 -> lift(villageName.trim()),
                _.reserve3 -> lift(batch())
              )
            }
          }
        }
      }
    }
  }

  val updateTable1Data = new Subcommand("uptable1") with InputFile {
    descr("更新附表1乡镇(街道)、村(社区)数据")

    val batch = opt[String](descr = "更新批次信息")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val file = new File(inputFile())

      if (file.isDirectory()) {
        file.listFiles.foreach { f =>
          if (f.isFile()) {
            updateFromFile(f)
          }
        }
      } else {
        updateFromFile(file)
      }

      def updateFromFile(file: File) = {
        println(s"更新自 $file")
        val workbook = Excel.load(file)
        val sheet = workbook.getSheetAt(0)
        transaction {
          for (row <- sheet.rowIterator(1)) {
            val idCard = row("B").value.trim()
            val countryName = row("E").value.trim()
            val villageName = row("F").value.trim()

            if (idCard != "" && countryName != "") {
              println(s"$idCard $countryName $villageName ${batch()}")
              if (batch.isEmpty) {
                run(
                  table1Data
                    .filter(_.idCard == lift(idCard))
                    .update(
                      _.reserve1 -> lift(countryName),
                      _.reserve2 -> lift(villageName)
                    )
                )
              } else {
                run(
                  table1Data
                    .filter(_.idCard == lift(idCard))
                    .update(
                      _.reserve1 -> lift(countryName),
                      _.reserve2 -> lift(villageName),
                      _.reserve3 -> lift(batch())
                    )
                )
              }
            }
          }
        }
      }
    }
  }

  val generateTable1Tables = new Subcommand("gntable1") {
    descr("生成参保和持卡情况核查表")

    val outputDir = """D:\数据核查\待遇核查回头看"""

    val template = outputDir / """参保与持卡情况表.xlsx"""

    val batch = trailArg[String](descr = "对应批次信息")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val destDir = outputDir / "参保与持卡情况表" / batch()

      println("生成参保和持卡情况核查表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val groups =
        run(
          table1Data
            .filter(d =>
              d.reserve1 != "" && d.reserve3 == lift(
                batch()
              )
            )
            .sortBy(d => (d.reserve1, d.reserve2))
            .groupBy(d => (d.reserve1, d.reserve2))
            .map { case (group, items) =>
              (group._1, group._2, items.size)
            }
        )

      transaction {
        var dwName = ""
        var total = 0
        for ((dw, cs, size) <- groups /*.take(4)*/ ) {
          if (dwName != dw) {
            val subTotal =
              groups.foldLeft(0)((n, e) =>
                if (e._1 == dw) n + e._3.toInt else n
              )
            total += subTotal
            println(s"\r\n$dw: $subTotal")
            Files.createDirectory(destDir / dw)
            dwName = dw
          }
          val dir = if (cs == "") "未分村社区" else cs
          println(s"  $dir: $size")
          Files.createDirectory(destDir / dw / dir)

          val items: List[Table1] =
            run {
              quote(
                infix"${table1Data.filter(d => d.reserve1 == lift(dw) && d.reserve2 == lift(cs) && d.reserve3 == lift(batch()))} ORDER BY CONVERT( name USING gbk )"
                  .as[Query[Table1]]
              )
            }

          val workbook = Excel.load(template)
          val sheet = workbook.getSheetAt(0)

          val startRow = 6
          var currentRow = startRow

          items.foreach { item =>
            val index = currentRow - startRow + 1
            val row = sheet.getOrCopyRow(currentRow, startRow)
            currentRow += 1

            //println(s"$index ${item.idCard}")

            row("A").value = index
            row("C").value = item.name
            row("D").value = item.idCard
            row("E").value = item.address
            if (item.dataType == "居保") {
              row("F").value = "√"
              row("H").value = "√"
            }
            if (item.cardNumber != "") {
              row("N").value = "√"
              row("P").value = item.cardNumber
              row("Q").value = item.bankName
            }
          }

          workbook.save(destDir / dw / dir / s"${dir}参保和持卡情况核查表.xlsx")
        }
        println(s"\r\n共计: ${total}")
      }
    }
  }

  /*var auditTable2Alive = new Subcommand("auditTable2Alive")
    with InputFile
    with RowRange {
    descr("审核附件2健在人员数据")

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      try {
        Session.use() { session =>
          for (index <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(index)
            val name = row("B").value
            val idCard = row("C").value
            print(s"${index + 1} $idCard $name ")
            val (error, reason, memo) =
              if (row("D").value != "是") {
                ("不健在", "", "")
              } else if (row("D").value != "是") {
                ("非本人持卡", "", "")
              } else {
                import yhsb.cjb.net.protocol.SessionOps.PersonStopAuditQuery

                val result = session.retiredPersonStopAuditQuery(idCard)
                if (result.nonEmpty) {
                  print("终止人员 ")
                  val item = result.head
                  //val stopTime = item.stopYearMonth
                  val reason = item.reason
                  print(s"${reason.toString} ")
                  if (reason != PayStopReason.JoinedEmployeeInsurance) {
                    ("非职保退休", reason.toString(), item.memo)
                  } else {
                    ("", "", "")
                  }
                } else {
                  val result = session.request(
                    RetiredPersonPauseQuery(idCard)
                  )
                  if (result.nonEmpty) {
                    print("暂停人员 ")
                    val item = result.head
                    val reason = item.reason
                    print(s"${reason.toString} ")
                    if (reason != PauseReason.NoLifeCertified) {
                      ("非未认证", reason.toString(), item.memo)
                    } else {
                      ("", "", "")
                    }
                  } else {
                    ("", "", "")
                  }
                }
              }

            if (error != "") {
              print(error)
              row.getOrCreateCell("T").value = error
              row.getOrCreateCell("U").value = reason
              row.getOrCreateCell("V").value = memo
            }

            println()
          }
        }
      } finally {
        workbook.save(inputFile().insertBeforeLast(".au"))
      }
    }
  }*/

  var auditTable2Alive = new Subcommand("auditTable2Alive")
    with InputFile
    with RowRange {
    descr("审核附件2健在人员数据")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      try {
        for (index <- (startRow() - 1) until endRow()) {
          val row = sheet.getRow(index)
          val name = row("B").value
          val idCard = row("C").value
          print(s"${index + 1} $idCard $name ")
          val (error, reason, memo) =
            if (row("D").value != "是") {
              val result: List[RetiredTable] = run(
                retiredData.filter(_.idCard == lift(idCard))
              )
              val item = result.head

              var (err, dtime) = ("", "")
              val deathDate = row("F").value.trim()
              if (deathDate != "") {
                item.retiredType match {
                  case "正常待遇" =>
                    print("正常待遇 ")
                    err = "死亡人员仍正常发放"
                  case "待遇暂停" =>
                    val stopTime = YearMonth.from(item.stopTime.toInt)
                    val deathTime = YearMonth.from(deathDate.toInt)
                    print(s"待遇暂停 $deathTime $stopTime ")
                    if (deathTime < stopTime.offset(-1)) {
                      err = "死亡时间早于暂停时间前一个月"
                      dtime = stopTime.toString()
                    }
                  case "待遇终止" =>
                    val stopTime = YearMonth.from(item.stopTime.toInt)
                    val deathTime = YearMonth.from(deathDate.toInt)
                    print(s"待遇终止 $deathTime $stopTime ")
                    if (deathTime < stopTime) {
                      err = "死亡时间早于终止时间"
                      dtime = stopTime.toString()
                    }
                }
              }
              if (err != "") {
                ("死亡时间早于系统时间", err, dtime)
              } else {
                if (row("H").value != "" && row("H").value != "无异常") {
                  ("异常情况", "", "")
                } else {
                  ("", "", "")
                }
              }
            } else if (row("E").value != "是") {
              ("非本人持卡", "", "")
            } else {
              var result: List[JbStopTable] = run(
                jbStopData.filter(f =>
                  f.idCard == lift(idCard) && f.dataType == "待遇暂停"
                )
              )
              if (result.nonEmpty) {
                print("暂停人员 ")
                val item = result.head
                val reason = PauseReason(item.stopReason)
                print(s"${reason.toString} ")
                if (reason != PauseReason.NoLifeCertified) {
                  ("非未认证停保", reason.toString(), item.memo)
                } else {
                  ("", "", "")
                }
              } else {
                result = run {
                  jbStopData
                    .filter(f =>
                      f.idCard == lift(idCard) && f.dataType == "待遇终止"
                    )
                    .sortBy(f => f.auditTime)(Ord.desc)
                }
                if (result.nonEmpty) {
                  print("终止人员 ")
                  val item = result.head
                  val reason = PayStopReason(item.stopReason)
                  print(s"${reason.toString} ")
                  if (reason != PayStopReason.JoinedEmployeeInsurance) {
                    ("非职保退休终止", reason.toString(), item.memo)
                  } else {
                    ("", "", "")
                  }
                } else {
                  ("", "", "")
                }
              }
            }

          if (error != "") {
            print(error)
            row.getOrCreateCell("T").value = error
            row.getOrCreateCell("U").value = reason
            row.getOrCreateCell("V").value = memo
          }

          println()
        }
      } finally {
        workbook.save(inputFile().insertBeforeLast(".au"))
      }
    }
  }

  var checkDeathDate = new Subcommand("checkDeathDate")
    with InputFile
    with RowRange {

    descr("检查死亡时间程序")

    val idCardRow = trailArg[String]("身份证号码列")
    val deathTimeRow = trailArg[String]("死亡时间列")
    val resultRow = trailArg[String]("结果输出列")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)
      for (index <- (startRow() - 1) until endRow()) {
        val row = sheet.getRow(index)
        val idCard = row(idCardRow()).value.trim()
        var deathDate = row(deathTimeRow()).value.trim()
        var error = ""
        if (!"""\d\d\d\d\d\d""".r.matches(deathDate)) {
          if (deathDate == "") {
            error = "死亡时间为空"
          } else {
            print(deathDate + " ")
            val regex = """^([12]\d\d\d)[\.\-\,\n]?(\d{1,2})[\.\-]?""".r
            regex.findFirstMatchIn(deathDate) match {
              case Some(value) =>
                val d = value.group(2)
                deathDate = value.group(1) + (if (d.length < 2) "0" + d else d)
                row(deathTimeRow()).value = deathDate
              case None =>
                error = "死亡日期格式有误"
            }
          }
        }
        if (error == "") {
          val result: List[RetiredTable] = run(
            retiredData.filter(_.idCard == lift(idCard))
          )
          if (result.isEmpty) {
            error = "非底册中数据"
          } else {
            val item = result.head
            item.retiredType match {
              case "正常待遇" =>
                println("正常待遇 ")
                error = "死亡人员仍正常发放"
              case "待遇暂停" =>
                val stopTime = YearMonth.from(item.stopTime.toInt)
                val deathTime = YearMonth.from(deathDate.toInt)
                println(s"待遇暂停 $deathTime $stopTime ")
                if (deathTime < stopTime.offset(-1)) {
                  error = s"死亡时间早于暂停时间前一个月(${stopTime.offset(-1)})"
                }
              case "待遇终止" =>
                val stopTime = YearMonth.from(item.stopTime.toInt)
                val deathTime = YearMonth.from(deathDate.toInt)
                println(s"待遇终止 $deathTime $stopTime ")
                if (deathTime < stopTime) {
                  error = s"死亡时间早于终止时间($stopTime)"
                }
            }
          }
        }
        row.getOrCreateCell(resultRow()).value = error
      }
      workbook.save(inputFile().insertBeforeLast("(比对结果)"))
    }
  }

  var auditTable2Error = new Subcommand("auditTable2Error")
    with InputFile
    with RowRange {
    descr("审核附件2异常数据")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)
      try {
        for (index <- (startRow() - 1) until endRow()) {
          val row = sheet.getRow(index)
          val name = row("B").value
          val idCard = row("C").value
          print(s"${index + 1} $idCard $name ")
          var error = ""
          if (row("D").value == "是" && row("E").value != "是") {
            error = "健在人员非本人领卡在手"
          } else if (row("D").value != "是") {
            val result: List[RetiredTable] = run(
              retiredData.filter(_.idCard == lift(idCard))
            )
            if (result.isEmpty) {
              error = "非底册中数据"
            } else {
              val item = result.head

              item.retiredType match {
                case "正常待遇" =>
                  error = "死亡人员仍正常发放"
                case "待遇暂停" =>
                  val stopTime = YearMonth.from(item.stopTime.toInt)
                  val deathTime = YearMonth.from(row("F").value.trim().toInt)
                  if (deathTime < stopTime.offset(-1)) {
                    error = "死亡时间早于暂停时间前一个月"
                  }
                case "待遇终止" =>
                  val stopTime = YearMonth.from(item.stopTime.toInt)
                  val deathTime = YearMonth.from(row("F").value.trim().toInt)
                  if (deathTime < stopTime) {
                    error = "死亡时间早于终止时间"
                  }
              }
              if (error == "") {
                if (row("H").value != "无异常") {
                  error = "异常情况"
                }
              }
            }
          }

          if (error != "") {
            print(error)
            row.getOrCreateCell("T").value = error
          }

          println()
        }
      } finally {
        workbook.save(inputFile().insertBeforeLast(".au"))
      }
    }
  }

  val loadVerifiedData = new Subcommand("ldverified") {
    descr("导入之前已核实数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    val fullcover = """D:\数据核查\待遇核查回头看\参保与持卡情况表\之前已核实的数据\全覆盖数据.xls"""
    val collegeStudent = """D:\数据核查\待遇核查回头看\参保与持卡情况表\之前已核实的数据\大学生.xls"""

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      import scala.jdk.CollectionConverters._

      if (clear()) {
        println("开始清除数据")
        run(verifiedData.delete)
        println("结束清除数据")
      }

      println(s"导入在校学生数据 $collegeStudent")
      Lookback2021.loadExcel(
        collegeStudentData.quoted,
        collegeStudent,
        2,
        fields = Seq("B", "C", "在校学生", "", "", "")
      )

      println(s"导入全覆盖数据 $fullcover")
      Lookback2021.loadExcel(
        fullcoverData.quoted,
        fullcover,
        2,
        fields = Seq("D", "C", "全覆盖", "H", "T", "")
      )

      println(s"合并到已核实数据表")
      def unionTable(tableName: String) = {
        println(s"开始合并 $tableName")
        val verifiedTable = verifiedData.quoted.name
        Lookback2021.execute(
          s"insert into $verifiedTable " +
            s"select * from $tableName " +
            s"on duplicate key update ${verifiedTable}.idcard=${verifiedTable}.idcard;",
          true
        )
        println(s"结束合并 $tableName")
      }
      unionTable(collegeStudentData.quoted.name)
      unionTable(fullcoverData.quoted.name)
    }
  }

  val loadStopData = new Subcommand("ldjbstop") with InputFile {
    descr("导入居保终止人员数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      import scala.jdk.CollectionConverters._

      if (clear()) {
        println("开始清除数据")
        run(jbStopData.filter(_.dataType == "待遇终止").delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      Lookback2021.loadExcel(
        jbStopData.quoted,
        inputFile(),
        2,
        fields = Seq("E", "D", "待遇终止", "I", "J", "P", "N")
      )
    }
  }

  val loadPauseData = new Subcommand("ldjbpause") with InputFile {
    descr("导入居保暂停人员数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      import scala.jdk.CollectionConverters._

      if (clear()) {
        println("开始清除数据")
        run(jbStopData.filter(_.dataType == "待遇暂停").delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      Lookback2021.loadExcel(
        jbStopData.quoted,
        inputFile(),
        2,
        fields = Seq("C", "D", "待遇暂停", "F", "G", "H", "")
      )
    }
  }

  val loadSSCompareData = new Subcommand("ldsscomp") with InputFile {
    descr("导入养老保险比对数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    val loadType = trailArg[String]("导入数据类型： 待遇人员、缴费人员")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._
      import scala.jdk.CollectionConverters._

      val ltype = loadType()
      if (clear()) {
        println(s"开始清除${ltype}数据")
        run(ssCompareData.filter(_.dataType == lift(ltype)).delete)
        println(s"结束清除${ltype}数据")
      }

      if (ltype == "待遇人员") {
        println(s"待遇人员导入 ${inputFile()}")
        Lookback2021.loadExcel(
          ssCompareData.quoted,
          inputFile(),
          4,
          fields = Seq("A", "B", "待遇人员", "E", "I", "K", "L")
        )
      } else if (ltype == "缴费人员") {
        println(s"缴费人员导入 ${inputFile()}")
        Lookback2021.loadExcel(
          ssCompareData.quoted,
          inputFile(),
          4,
          fields = Seq("A", "B", "缴费人员", "E", "J", "L", "M")
        )
      }
    }
  }

  val exportTable1DataByDw = new Subcommand("extable1dw") {
    descr("生成参保和持卡情况核查表")

    val outputDir = """D:\数据核查\待遇核查回头看"""

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val template = outputDir / "参保与持卡情况表" / "居保附表1汇总表模板.xlsx"

      val destDir = outputDir / "参保与持卡情况表" / "汇总表"

      println("生成参保和持卡情况核查汇总表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val groups =
        run(
          table1CompareData
            .groupBy(d => (d.reserve1))
            .map { case (group, items) =>
              (group, items.size)
            }
        )

      var total: Long = 0
      for ((dw, size) <- groups) {
        Files.createDirectory(destDir / dw)
        println(s"$dw: $size")
        total += size
        val items: List[Table1CompareResult] = run {
          quote(
            infix"${table1CompareData.filter(_.reserve1 == lift(dw))} ORDER BY CONVERT( reserve2 USING gbk ), CONVERT( name USING gbk )"
              .as[Query[Table1CompareResult]]
          )
        }

        Excel.exportWithTemplate[Table1CompareResult](
          items,
          template.toString(),
          1,
          (destDir / dw / s"${dw}居保附表1汇总表($size).xlsx").toString,
          (index, row, item) => {
            row("A").value = index
            row("B").value = item.name
            row("C").value = item.idCard
            row("D").value = item.address
            row("E").value = item.reserve1
            row("F").value = item.reserve2
            row("G").value = if (item.dataType == "居保") "是" else "否"
            row("H").value = if (item.bankName != "") "国家社保卡" else ""
            row("I").value = item.bankName
            row("J").value = item.cardNumber
            row("K").value = if (item.resultDataType != "") "已参保人员" else ""
            row("L").value = if (item.resultDataType != "") {
              s"${item.resultDataType},${item.resultType},${item.resultArea}"
            } else ""
          }
        )
      }
      println(s"合计: $total")
    }
  }

  val table41Classify = new Subcommand("table41Classify")
    with InputFile
    with RowRange {
    descr("居保附表2分类程序")

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)
      for {
        index <- (startRow() - 1) until endRow()
        row = sheet.getRow(index)
      } {
        val idCard = row("E").value
        val name = row("F").value
        val alive = row("N").value

        var kind = ""

        print(s"$index $idCard $name ")

        // 1. 是否健在人员非本人持卡
        if (kind == "") {
          if (alive == "是") {
            val keepCard = row("O").value
            if (keepCard == "否") {
              kind = "健在非本人持卡"
            }
          }
        }

        // 2. 死亡冒领
        if (kind == "") {
          if (alive == "否") {
            val state = row("K").value
            val relativeKeepCard = row("Q").value == "是"
            state match {
              case "正常待遇" =>
                if (relativeKeepCard) {
                  kind = "亲属持卡冒领"
                } else {
                  kind = "非亲属持卡冒领"
                }
              case "待遇暂停" =>
                val stopTime = YearMonth.from(row("L").value.toInt)
                val deathTime = YearMonth.from(row("P").value.toInt)
                if (deathTime < stopTime.offset(-1)) {
                  if (relativeKeepCard) {
                    kind = "亲属持卡冒领"
                  } else {
                    kind = "非亲属持卡冒领"
                  }
                }
              case "待遇终止" =>
                val stopTime = YearMonth.from(row("L").value.toInt)
                val deathTime = YearMonth.from(row("P").value.toInt)
                if (deathTime < stopTime) {
                  if (relativeKeepCard) {
                    kind = "亲属持卡冒领"
                  } else {
                    kind = "非亲属持卡冒领"
                  }
                }
            }
          }
        }

        // 3. 异常人员
        if (kind == "") {
          val error = row("R").value
          if (error != "" && error != "无异常") {
            kind = "异常人员"
          }
        }

        // 4. 正常人员
        if (kind == "") {
          kind = "正常人员"
        }

        println(kind)

        row.getOrCreateCell("X").value = kind
      }
      workbook.save(inputFile().insertBeforeLast(".up"))
    }
  }

  val table41Refund = new Subcommand("table41Refund")
    with InputFile
    with RowRange {
    descr("居保附表2涉及冒领金额测算")

    def execute(): Unit = {
      Session.use("004") { session =>
        def getRefundData(
            idCard: String,
            deathTime: String
        ): (String, Int) = {
          val deathYearMonth = deathTime.toInt
          val endYearMonth = Formatter.formatDate("yyyyMM").toInt
          val item = session.request(PersonInfoQuery(idCard)).head
          var (amount, months) = ("", 0)
          amount = item.cbState match {
            case CBState.Paused | CBState.Normal =>
              session
                .request(
                  PaymentTerminateQuery(
                    item,
                    f"$deathYearMonth%06d",
                    PayStopReason.Death
                  )
                )
                .headOption
                .map(_.auditAmount)
                .getOrElse("")
            case _ => ""
          }
          var payAmount = BigDecimal(0)
          var monthSet = mutable.TreeSet[Int]()
          for (
            item <- session.request(PersonInfoPaylistQuery(item))
            if item.payYearMonth > deathYearMonth &&
              item.payYearMonth <= endYearMonth &&
              item.payItem.startsWith("基础养老金") &&
              item.payState == "已支付"
          ) {
            payAmount += item.amount
            monthSet.addOne(item.payYearMonth)
          }
          months = monthSet.size
          println(s"$idCard $amount $payAmount $months")
          (
            if (amount != "") amount else payAmount.toString,
            months
          )
        }

        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)
        try {
          for {
            index <- (startRow() - 1) until endRow()
            row = sheet.getRow(index)
          } {
            val idCard = row("E").value
            val name = row("F").value
            val kind = row("X").value

            val refund = kind match {
              case "健在非本人持卡" =>
                val (refund, _) = getRefundData(idCard, "201106")
                row.getOrCreateCell("Y").value = refund
                refund
              case "亲属持卡冒领" =>
                val deathTime = row("P").value
                val (refund, months) = getRefundData(idCard, deathTime)
                row.getOrCreateCell("Z").value = months
                row.getOrCreateCell("AA").value = refund
                refund
              case "非亲属持卡冒领" =>
                val deathTime = row("P").value
                val (refund, months) = getRefundData(idCard, deathTime)
                row.getOrCreateCell("Z").value = months
                row.getOrCreateCell("AB").value = refund
                refund
              case "异常人员" =>
                val (refund, _) = getRefundData(idCard, "201106")
                row.getOrCreateCell("AC").value = refund
                refund
              case _ => ""
            }

            println(s"$index $idCard $name $kind $refund")
          }
        } finally {
          workbook.save(inputFile().insertBeforeLast(".up"))
        }
      }
    }
  }

  val table41Refunded = new Subcommand("table41Refunded")
    with InputFile
    with RowRange {
    descr("统计已稽核金额")

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      try {
        Session.use("003") { session =>
          for {
            index <- (startRow() - 1) until endRow()
            row = sheet.getRow(index)
          } {
            val name = row("B").value
            val idCard = row("C").value

            var total = BigDecimal(0)
            var time = 0

            for (item <- session.request(RefundQuery(idCard))) {
              if (item.state == "已到账") {
                total += item.amount
                val rtime = item.refundedTime.split("-").take(2).mkString.toInt
                if (rtime > time) {
                  time = rtime
                }
              }
            }

            if (total > 0) {
              row.getOrCreateCell("O").value = total
              row.getOrCreateCell("P").value = time
            }

            println(s"$index $idCard $name $total $time")
          }
        }
      } finally {
        workbook.save(inputFile().insertBeforeLast(".up"))
      }
    }
  }

  val loadTable1VerifiedData = new Subcommand("loadTable1Verified")
    with InputFile {
    descr("导入附表1已核实数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(table1VerifiedData.delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      Lookback2021.loadExcel(
        table1VerifiedData.quoted,
        inputFile(),
        2,
        fields = Seq("D", "C", "B", "K", "J", "", "", "", "")
      )
    }
  }

  val exportTable1VerifiedAllData = new Subcommand("exportTable1Verifed") {
    descr("生成参保和持卡情况核查表")

    val outputDir = """D:\数据核查\待遇核查回头看"""

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val template = outputDir / "参保与持卡情况表" / "居保附表1汇总表模板.xlsx"

      val destDir = outputDir / "参保与持卡情况表" / "未核实数据表"

      println("生成参保和持卡情况未核实数据汇总表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val groups =
        run(
          table1VerifiedAllData
            .filter(f => f.verified != "是" && f.resultDataType == "")
            .groupBy(d => (d.reserve1))
            .map { case (group, items) =>
              (group, items.size)
            }
        )

      var total: Long = 0
      for ((dw, size) <- groups) {
        Files.createDirectory(destDir / dw)
        println(s"$dw: $size")
        total += size
        val items: List[Table1VerifiedResult] = run {
          quote(
            infix"${table1VerifiedAllData.filter(f => f.verified != "是" && f.resultDataType == "" && f.reserve1 == lift(dw))} ORDER BY CONVERT( reserve2 USING gbk ), CONVERT( name USING gbk )"
              .as[Query[Table1VerifiedResult]]
          )
        }

        Excel.exportWithTemplate[Table1VerifiedResult](
          items,
          template.toString(),
          1,
          (destDir / dw / s"${dw}居保附表1未核实数据汇总表($size).xlsx").toString,
          (index, row, item) => {
            row("A").value = index
            row("B").value = item.name
            row("C").value = item.idCard
            row("D").value = item.address
            row("E").value = item.reserve1
            row("F").value = item.reserve2
            row("G").value = if (item.dataType == "居保") "是" else "否"
            row("H").value = if (item.bankName != "") "国家社保卡" else ""
            row("I").value = item.bankName
            row("J").value = item.cardNumber
            row("K").value = if (item.resultDataType != "") "已参保人员" else ""
            row("L").value = if (item.resultDataType != "") {
              s"${item.resultDataType},${item.resultType},${item.resultArea}"
            } else ""
          }
        )
      }
      println(s"合计: $total")
    }
  }

  val exportTable1ImportData = new Subcommand("exportTable1Import") {
    descr("生成参保和持卡情况导库表")

    val outputDir = """D:\数据核查\待遇核查回头看"""

    val dwName = trailArg[String]("单位名称")
    val operator = trailArg[String]("核实人姓名")
    val phone = trailArg[String]("核实人联系电话")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val dw = dwName()

      val template = outputDir / "附表1导库生成数据" / "居保附表1导库模板.xls"

      val destDir = outputDir / "附表1导库生成数据" / "居保附表1导库文件" / dwName()

      println("生成参保和持卡情况导库表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val items: List[Table1VerifiedResult] = run {
        quote(
          infix"${table1VerifiedAllData.filter(f => f.verified != "是" && f.resultDataType != "" && f.reserve1 == lift(dw))} ORDER BY CONVERT( reserve2 USING gbk ), CONVERT( name USING gbk )"
            .as[Query[Table1VerifiedResult]]
        )
      }

      Excel.exportWithTemplate[Table1VerifiedResult](
        items,
        template.toString(),
        1,
        (destDir / s"${dw}居保附表1导库文件.xls").toString,
        (index, row, item) => {
          row("A").value = "430302"
          row("B").value = item.name
          row("C").value = item.idCard
          row("D").value = item.address
          row("E").value = "0"
          row("F").value = ""
          row("G").value = ""
          row("H").value = ""
          row("I").value = ""
          row("J").value = ""
          row("K").value = ""
          row("L").value = ""
          row("M").value = "1"
          row("N").value = {
            val area = if (item.resultArea.length > 14) {
              item.resultArea.substring(0, 14)
            } else {
              item.resultArea
            }
            item.resultType match {
              case "城镇企业职工基本养老保险" =>
                s"职保参保($area)"
              case "机关事业单位养老保险" =>
                s"机保参保($area)"
              case "城乡居民社会养老保险" | "新型农村社会养老保险" =>
                s"异地居保($area)"
              case _ => ""
            }
          }

          row("O").value = operator()
          row("P").value = phone()
        },
        1000
      )
    }
  }

  val lookbackStatics = new Subcommand("lookbackStatics") {
    descr("生成回头看核查统计数据")

    val filePath = """D:\数据核查\待遇核查回头看\养老待遇核查“回头看”数据录入情况汇总表.xls"""

    def execute(): Unit = {
      val workbook = Excel.load(filePath)
      val sheet = workbook.getSheetAt(0)
      val mapSheet = workbook.getSheetAt(1)

      def loadMap() = {
        val map = mutable.LinkedHashMap[String, Seq[String]]()
        for {
          index <- 1 to 12
          row = mapSheet.getRow(index)
        } {
          map(row("A").value) = Seq(row("B").value, row("C").value)
        }
        map
      }
      val map = loadMap()

      var (table1Total, table2Total) = (0, 0)
      Session.use() { session =>
        for {
          index <- 3 to 14
          row = sheet.getRow(index)
        } {
          var (dwTable1Total, dwTable2Total) = (0, 0)
          val dwName = row("A").value
          for {
            operator <- map(dwName)
            if operator != ""
          } {
            dwTable1Total += session
              .request(LookBackTable1Audit(operator))
              .rowcount
            dwTable2Total += session
              .request(LookBackTable2Audit(operator))
              .rowcount
          }
          println(s"$dwName $dwTable1Total $dwTable2Total")
          row("E").value = dwTable1Total
          row("I").value = dwTable2Total
          row("J").setCellFormula(
            s"(G${index + 1}+I${index + 1})/(F${index + 1}+H${index + 1})"
          )

          table1Total += dwTable1Total
          table2Total += dwTable2Total
        }
        sheet("E16").setCellFormula("SUM(E4:E15)")
        sheet("I16").setCellFormula("SUM(I4:I15)")
        println(s"合计 $table1Total $table2Total")
      }
      workbook.save(filePath.insertBeforeLast(s".${Formatter.formatDate()}"))
    }
  }

  val table2Cancel = new Subcommand("table2Cancel")
    with InputFile
    with RowRange {
    descr("居保附表2作废程序")

    val operator = trailArg[String]("操作员")
    val nameCol = trailArg[String]("姓名列")
    val idCardCol = trailArg[String]("身份证列")
    val messageCol = trailArg[String]("反馈信息列")
    val memo = trailArg[String]("作废原因")

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)
      Session.use() { session =>
        for {
          index <- (startRow() - 1) until endRow()
          row = sheet.getRow(index)
        } {
          val name = row(nameCol()).value
          val idCard = row(idCardCol()).value
          val message = if (idCard == "") {
            "身份号码不能为空"
          } else {
            session
              .request(LookBackTable2Audit(operator(), idCard))
              .headOption match {
              case None       => "系统中未查到该人核实信息"
              case Some(item) =>
                //println(item)
                //session.toService(LookBackTable2Cancel(item, memo()))
                session.request(LookBackTable2Cancel(item, memo())).message
            }
          }
          println(s"$idCard $name $message")
        }
      }
    }
  }

  val loadTable2VerifiedData = new Subcommand("loadTable2Verified")
    with InputFile {
    descr("导入附表2已核实数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(table2VerifiedData.delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      Lookback2021.loadExcel(
        table2VerifiedData.quoted,
        inputFile(),
        2,
        fields = ('A' to 'T').map(c => s"$c")
      )
    }
  }

  val loadOutsideDeathData = new Subcommand("loadOutsideDeathData")
    with InputFile {
    descr("导入外部死亡数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      if (clear()) {
        println("开始清除数据")
        run(outsideDeathData.delete)
        println("结束清除数据")
      }

      println(s"导入 ${inputFile()}")
      Lookback2021.loadExcel(
        outsideDeathData.quoted,
        inputFile(),
        2,
        fields = Seq("E", "F", "L", "G")
      )
    }
  }

  def checkTable2Template(
      name: String,
      idCard: String,
      alive: String,
      keepCardBySelf: String,
      deathDate: String,
      keepCardByRelative: String,
      abnormalType: String,
      abnormalDetail: String
  ) = {
    import yhsb.cjb.db.lookback.Lookback2021._

    val items: List[Table2VerifiedResult] =
      run(table2VerifiedData.filter(_.idCard == lift(idCard)))

    val kindOld = items.headOption match {
      case None => "错误:身份证号码有误"
      case Some(value) => {
        value.alive match {
          case "是" => "健在"
          case "否" => "死亡"
          case ""  => "异常情况"
          case _   => "错误:未知类型"
        }
      }
    }

    val kindNew = if (alive == "1") {
      if (deathDate != "") {
        "错误:健在人员死亡日期必为空"
      } else if (abnormalType != "0") {
        "错误:健在人员异常情况必填0(无异常)"
      } else {
        "健在人员"
      }
    } else if (alive == "0") {
      if (deathDate == "") {
        if (abnormalType == "0") {
          "错误:异常人员异常情况必不为0(无异常)"
        } else if (abnormalType == "4" && abnormalDetail.trim() == "") {
          "错误:其他异常情况人员核查结果备注必填"
        } else {
          "异常情况人员"
        }
      } else {
        if (abnormalType != "0") {
          "错误:死亡人员异常情况必为0(无异常)"
        } else {
          if (!"""\d\d\d\d\d\d""".r.matches(deathDate)) {
            "错误:死亡日期有误"
          } else {
            "死亡人员"
          }
        }
      }
    } else if (alive == "") {
      if (deathDate != "") {
        "错误:异常情况人员死亡时间必为空"
      } else if (abnormalType == "0") {
        "错误:异常人员异常情况必不为0(无异常)"
      } else if (abnormalType == "4" && abnormalDetail.trim() == "") {
        "错误:其他异常情况人员核查结果备注必填"
      } else {
        "异常情况人员"
      }
    } else {
      "错误:是否健在填写有误"
    }

    var checkResult = ""
    var sysStopTime = ""
    var sysPauseTime = ""
    if (
      kindNew == "死亡人员" && items.nonEmpty &&
      """\d\d\d\d\d\d""".r.matches(
        deathDate
      )
    ) {
      val item = items.head
      item.payState match {
        case "正常待遇" =>
          checkResult = "死亡人员仍正常发放"
        case "待遇暂停" =>
          val stopTime = YearMonth.from(item.stopTime.toInt)
          val deathTime = YearMonth.from(deathDate.toInt)
          if (deathTime < stopTime.offset(-1)) {
            checkResult = "死亡时间早于暂停时间前一个月"
            sysPauseTime = stopTime.toString
          }
        case "待遇终止" =>
          val stopTime = YearMonth.from(item.stopTime.toInt)
          val deathTime = YearMonth.from(deathDate.toInt)
          if (deathTime < stopTime) {
            checkResult = "死亡时间早于终止时间"
            sysStopTime = stopTime.toString
          }
      }
    }

    (kindOld, kindNew, checkResult, sysStopTime, sysPauseTime)
  }

  val checkRevisionData = new Subcommand("checkRevisionData")
    with InputFile
    with RowRange {
    descr("检查复核和修正数据")

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      for {
        index <- (startRow() - 1) until endRow()
        row = sheet.getRow(index)
      } {
        val name = row("A").value
        val idCard = row("B").value

        val alive = row("C").value
        val keepCardBySelf = row("D").value
        val deathDate = row("E").value.trim()
        val keepCardByRelative = row("F").value
        val abnormalType = row("G").value
        val abnormalDetail = row("H").value

        val (kindOld, kindNew, checkResult, _, _) = checkTable2Template(
          name,
          idCard,
          alive,
          keepCardBySelf,
          deathDate,
          keepCardByRelative,
          abnormalType,
          abnormalDetail
        )
        row.getOrCreateCell("M").value = kindOld
        row.getOrCreateCell("N").value = kindNew
        row.getOrCreateCell("O").value = checkResult
      }

      workbook.save(inputFile().insertBeforeLast(".up"))
    }
  }

  val mergeTable2Templates = new Subcommand("mergeTable2Templates")
    with InputDir
    with InputFile {

    val outputDir = """D:\数据核查\回头看数据复核\上报数据"""
    val template = outputDir / "合并数据模板.xls"

    def execute(): Unit = {
      val inputFiles = listFiles(new File(inputDir()), """.*\.xls""")

      val outWorkbook = Excel.load(template)
      var outSheet = outWorkbook.getSheetAt(0)

      val fields = ('A' to 'L').map(c => s"$c")
      var startRow, currentRow = 1

      Session.use("003") { session =>
        for (file <- inputFiles) {
          var dwName = file.getName()
          dwName = dwName.substring(0, dwName.length - 4)

          println(s"合并 $dwName")

          val workbook = Excel.load(file)
          val sheet = workbook.getSheetAt(0)

          for (index <- 1 to sheet.getLastRowNum) {
            val row = sheet.getRow(index)
            val name = row("A").value.trim()
            val idCard = row("B").value.trim()

            if (name != "" && idCard != "") {
              val outRow = outSheet.getOrCopyRow(currentRow, startRow)
              currentRow += 1
              outRow.getOrCreateCell("M").value = dwName
              row.copyTo(outRow, fields: _*)

              val alive = row("C").value
              val keepCardBySelf = row("D").value
              val deathDate = row("E").value.trim()
              val keepCardByRelative = row("F").value
              val abnormalType = row("G").value
              val abnormalDetail = row("H").value

              val (kindOld, kindNew, checkResult, stopTime, pauseTime) =
                checkTable2Template(
                  name,
                  idCard,
                  alive,
                  keepCardBySelf,
                  deathDate,
                  keepCardByRelative,
                  abnormalType,
                  abnormalDetail
                )

              outRow.getOrCreateCell("N").value = kindOld
              outRow.getOrCreateCell("O").value = kindNew
              outRow.getOrCreateCell("P").value = checkResult
              outRow.getOrCreateCell("Q").value = stopTime
              outRow.getOrCreateCell("R").value = pauseTime

              if (stopTime != "" || pauseTime != "") {
                var total = BigDecimal(0)
                var time = 0

                for (item <- session.request(RefundQuery(idCard))) {
                  if (item.state == "已到账") {
                    total += item.amount
                    val rtime =
                      item.refundedTime.split("-").take(2).mkString.toInt
                    if (rtime > time) {
                      time = rtime
                    }
                  }
                }

                if (total > 0) {
                  outRow.getOrCreateCell("S").value = total
                  outRow.getOrCreateCell("T").value = time
                }
              }
            }
          }
        }
      }
      outWorkbook.save(inputFile())
    }
  }

  val unmergeTable2Templates = new Subcommand("unmergeTable2Templates")
    with InputFile
    with OutputDir {

    val rootDir = """D:\数据核查\回头看数据复核\上报数据\"""
    val template = rootDir / "分解数据模板.xls"

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      println("生成分组映射表")
      val map = mutable.LinkedHashMap[String, mutable.ListBuffer[Int]]()
      for (index <- 1 to sheet.getLastRowNum()) {
        val list = map.getOrElseUpdate(
          sheet.getRow(index)("M").value,
          mutable.ListBuffer()
        )
        list.addOne(index)
      }

      println("分解已修改数据")
      if (Files.exists(outputDir())) {
        Files.move(outputDir(), s"${outputDir()}.orig")
      }
      Files.createDirectory(outputDir())

      val fields = ('A' to 'L').map(c => s"$c")

      var total = 0
      for ((dw, indexes) <- map) {
        val subTotal = indexes.size
        total += subTotal
        println(s"\r\n$dw: ${subTotal}")

        val outWorkbook = Excel.load(template)
        val outSheet = outWorkbook.getSheetAt(0)
        val startRow = 1
        var currentRow = startRow

        indexes.foreach { rowIndex =>
          val index = currentRow - startRow + 1
          val inRow = sheet.getRow(rowIndex)

          val outRow = outSheet.getOrCopyRow(currentRow, startRow)
          currentRow += 1
          inRow.copyTo(outRow, fields: _*)
        }

        outWorkbook.save(outputDir() / s"${dw}.xls")
      }

      println(s"\r\n共计: ${total}")
    }
  }

  val exportTable1Result = new Subcommand("exportTable1Result") {
    descr("下载回头看附表1结果数据")

    val outputDir = """D:\数据核查\回头看数据核查\导出附表1结果"""

    override def execute(): Unit = {
      for ((code, name) <- Division.validCodeMap) {
        println(s"开始导出 $name 附表1结果")

        val exportFile = Files.createTempFile("yhsb", ".xls").toString
        Session.use() {
          _.exportAllTo(
            LookBackTable1Query(code),
            LookBackTable1Query.columnMap
          )(
            exportFile
          )
        }

        val workbook = Excel.load(exportFile)
        val sheet = workbook.getSheetAt(0)
        sheet.setColumnWidth(1, 35 * 256)
        sheet.setColumnWidth(3, 19 * 256)

        workbook.saveAfter(
          outputDir / s"${name}附表1结果${Formatter.formatDate()}.xls"
        ) { path =>
          println(s"保存: $path")
        }
      }

      println("结束数据导出")
    }
  }

  val exportTable2Result = new Subcommand("exportTable2Result") {
    descr("下载回头看附表2结果数据")

    val outputDir = """D:\数据核查\回头看数据核查\导出附表2结果"""

    override def execute(): Unit = {
      for ((code, name) <- Division.codeMap) {
        println(s"开始导出 $name 附表2结果")

        val exportFile = Files.createTempFile("yhsb", ".xls").toString
        Session.use() {
          _.exportAllTo(
            LookBackTable2Query(code),
            LookBackTable2Query.columnMap
          )(
            exportFile
          )
        }

        val workbook = Excel.load(exportFile)
        val sheet = workbook.getSheetAt(0)
        sheet.setColumnWidth(0, 19 * 256)
        sheet.setColumnWidth(1, 19 * 256)
        sheet.setColumnWidth(2, 9 * 256)
        sheet.setColumnWidth(4, 20 * 256)
        sheet.setColumnWidth(5, 12 * 256)
        sheet.setColumnWidth(6, 12 * 256)

        workbook.saveAfter(
          outputDir / s"${name}附表2结果${Formatter.formatDate()}.xls"
        ) { path =>
          println(s"保存: $path")
        }
      }

      println("结束数据导出")
    }
  }

  val checkCardsData = new Subcommand("checkCardsData")
    with InputFile
    with RowRange {
    descr("检查社保卡数据")

    def execute(): Unit = {
      import yhsb.cjb.db.lookback.Lookback2021._

      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      for {
        index <- (startRow() - 1) until endRow()
        row = sheet.getRow(index)
        idCard = row("E").value
      } {
        println(s"$idCard")
        val cards: List[Table1] = run(cardData.filter(_.idCard == lift(idCard)))
        if (cards.nonEmpty) {
          val card = cards.head
          row.getOrCreateCell("P").value = card.name
          row.getOrCreateCell("Q").value = card.bankName
          row.getOrCreateCell("R").value = card.cardNumber
          row.getOrCreateCell("S").value =
            if (row("O").value == card.cardNumber) {
              "是"
            } else {
              "否"
            }
        }
      }

      workbook.save(inputFile().insertBeforeLast(".upd"))
    }
  }

  val swydkTables = new Subcommand("swydk") with InputFile with RowRange {
    descr("死亡待遇人员疑点卡核查明细表")

    val outputDir = """D:\数据核查\回头看数据核查\深化整改核实"""

    val template = outputDir / """死亡待遇人员疑点卡核查明细表模板.xls"""

    def execute(): Unit = {
      val destDir = outputDir / "死亡待遇人员疑点卡"

      println("生成死亡待遇人员疑点卡核查明细表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

      val map = mutable
        .LinkedHashMap[String, mutable.Map[String, mutable.ListBuffer[Int]]]()

      for (index <- (startRow() - 1) until endRow()) {
        val row = sheet.getRow(index)
        val countryName = row("B").value.trim()
        var villageName = row("C").value.trim()
        val subMap = map.getOrElseUpdate(countryName, mutable.LinkedHashMap())
        val list = subMap.getOrElseUpdate(villageName, mutable.ListBuffer())
        list.addOne(index)
      }

      def createTable(savePath: Path, list: Iterable[Int]) = {
        val outWorkbook = Excel.load(template)
        val outSheet = outWorkbook.getSheetAt(0)

        val startRow = 3
        var currentRow = startRow

        list.foreach { rowIndex =>
          val index = currentRow - startRow + 1
          val sourceRow = sheet.getRow(rowIndex)
          val outRow = outSheet.getOrCopyRow(currentRow, startRow)
          currentRow += 1
          outRow("A").value = index
          outRow("B").value = sourceRow("B").value
          outRow("C").value = sourceRow("C").value
          outRow("D").value = sourceRow("D").value
          outRow("E").value = sourceRow("E").value
          outRow("F").value = sourceRow("F").value
          outRow("G").value = sourceRow("G").value
          outRow("H").value = sourceRow("H").value
          outRow("I").value = sourceRow("I").value
          outRow("J").value = sourceRow("J").value
        }
        outWorkbook.save(savePath)
      }

      var total = 0
      for ((dw, subMap) <- map) {
        Files.createDirectory(destDir / dw)
        
        val list = subMap.values.flatMap(_.toIterable)
        val subTotal = list.size
        createTable(destDir / dw / s"${dw}死亡待遇人员疑点卡核查明细表.xls", list)
        
        total += subTotal
        println(s"\r\n$dw: $subTotal")

        for ((cs, list) <- subMap) {
          println(s"  $cs: ${list.size}")
          Files.createDirectory(destDir / dw / cs)
          createTable(destDir / dw / cs / s"${cs}死亡待遇人员疑点卡核查明细表.xlsx", list)
        }
      }
      println(s"\r\n共计: ${total}")
    }
  }

  addSubCommand(retiredTables)

  addSubCommand(zipSubDir)

  addSubCommand(loadCardsData)
  addSubCommand(loadJbData)
  addSubCommand(loadQmcbData)
  addSubCommand(loadRetiredData)
  addSubCommand(loadPoliceData)
  addSubCommand(exportPoliceData)

  addSubCommand(unionAllData)

  addSubCommand(deleteRetired)
  addSubCommand(updateDwAndCs)

  addSubCommand(cbckTables)

  addSubCommand(qbcbckTables)

  addSubCommand(generateAddressTable)

  addSubCommand(unionTable1Data)
  addSubCommand(mapTable1Data)
  addSubCommand(insertTable1Data)
  addSubCommand(exportTable1Data)
  addSubCommand(updateTable1Data)
  addSubCommand(generateTable1Tables)

  addSubCommand(auditTable2Alive)
  addSubCommand(auditTable2Error)
  addSubCommand(checkDeathDate)

  addSubCommand(loadVerifiedData)

  addSubCommand(loadStopData)
  addSubCommand(loadPauseData)

  addSubCommand(loadSSCompareData)
  addSubCommand(exportTable1DataByDw)

  addSubCommand(table41Classify)
  addSubCommand(table41Refund)
  addSubCommand(table41Refunded)

  addSubCommand(loadTable1VerifiedData)

  addSubCommand(exportTable1VerifiedAllData)

  addSubCommand(exportTable1ImportData)

  addSubCommand(lookbackStatics)

  addSubCommand(table2Cancel)

  addSubCommand(loadTable2VerifiedData)
  addSubCommand(loadOutsideDeathData)

  addSubCommand(retiredTables2)

  addSubCommand(checkRevisionData)
  addSubCommand(mergeTable2Templates)

  addSubCommand(unmergeTable2Templates)

  addSubCommand(exportTable1Result)
  addSubCommand(exportTable2Result)

  addSubCommand(checkCardsData)

  addSubCommand(swydkTables)
}
