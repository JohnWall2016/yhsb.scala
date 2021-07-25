import yhsb.base.command.Command
import yhsb.base.command.Subcommand
import yhsb.base.command.InputFile
import yhsb.base.command.RowRange

import yhsb.base.excel.Excel._
import yhsb.base.io.Path._

import yhsb.base.text.String._

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
import scala.collection.mutable.ListBuffer
import yhsb.cjb.net.protocol.Division
import scala.util.matching.Regex
import yhsb.cjb.db.LBTable1
import io.getquill.Query
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.RetiredPersonStopAuditQuery
import yhsb.cjb.net.protocol.PayStopReason
import yhsb.cjb.net.protocol.RetiredPersonPauseQuery
import yhsb.cjb.net.protocol.PauseReason

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

  val loadCardsData = new Subcommand("ldcards") with InputDir {
    descr("导入社保卡数据")

    val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))

    val reserve1 = opt[String](descr = "reserve1字段设置内容", default = Some(""))

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
          fields = Seq("C", "B", "D", "I", "J", "社保卡", reserve1(), "", "")
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
      import yhsb.cjb.db.Lookback2021._

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
      import yhsb.cjb.db.Lookback2021._

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
      import yhsb.cjb.db.Lookback2021._
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
      import yhsb.cjb.db.Lookback2021._
      import scala.jdk.CollectionConverters._

      val items: List[LBTable1] = run {
        quote {
          infix"$policeData #${condition()}".as[Query[LBTable1]]
        }
      }

      println(items.size)

      Excel.export[LBTable1](
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
      import yhsb.cjb.db.Lookback2021._
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
      import yhsb.cjb.db.Lookback2021._

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
      import yhsb.cjb.db.Lookback2021._

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

        val items: List[LBTable1] = run {
          quote(
            infix"${unionData.filter(d => d.reserve1 == lift(dw) && d.reserve2 == lift(cs))} ORDER BY CONVERT( name USING gbk )"
              .as[Query[LBTable1]]
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
      import yhsb.cjb.db.Lookback2021._

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
      import yhsb.cjb.db.Lookback2021._

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
      import yhsb.cjb.db.Lookback2021._
      import scala.jdk.CollectionConverters._

      val items: List[LBTable1] = run {
        quote {
          infix"$table1Data #${condition()}".as[Query[LBTable1]]
        }
      }

      println(items.size)

      Excel.export[LBTable1](
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

  val updateTable1Data = new Subcommand("uptable1") with InputFile {
    descr("更新附表1乡镇(街道)、村(社区)数据")

    def execute(): Unit = {
      import yhsb.cjb.db.Lookback2021._

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

            if (idCard != "" && countryName != "" && villageName != "") {
              println(s"$idCard $countryName $villageName")
              run(
                table1Data
                  .filter(_.idCard == lift(idCard))
                  .update(
                    _.reserve1 -> lift(countryName),
                    _.reserve2 -> lift(villageName)
                  )
              )
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

    def execute(): Unit = {
      import yhsb.cjb.db.Lookback2021._

      val destDir = outputDir / "参保与持卡情况表" / "第二批"

      println("生成参保和持卡情况核查表")
      if (Files.exists(destDir)) {
        Files.move(destDir, s"${destDir.toString}.orig")
      }
      Files.createDirectory(destDir)

      val groups = run(
        table1Data
          .filter(d =>
            d.reserve1 != "" && d.reserve2 != "" /*&& d.reserve2 == "羊牯塘社区"*/
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
          println(s"  $cs: $size")
          Files.createDirectory(destDir / dw / cs)

          val items: List[LBTable1] = run {
            quote(
              infix"${table1Data.filter(d => d.reserve1 == lift(dw) && d.reserve2 == lift(cs))} ORDER BY CONVERT( name USING gbk )"
                .as[Query[LBTable1]]
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

          workbook.save(destDir / dw / cs / s"${cs}参保和持卡情况核查表.xlsx")
        }
        println(s"\r\n共计: ${total}")
      }
    }
  }

  var auditTable2 = new Subcommand("auditTable2") with InputFile with RowRange {
    descr("审核附件2")

    def execute(): Unit = {
      val workbook = Excel.load(inputFile())
      val sheet = workbook.getSheetAt(0)

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
      workbook.save(inputFile().insertBeforeLast(".au"))
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

  addSubCommand(generateAddressTable)

  addSubCommand(unionTable1Data)
  addSubCommand(mapTable1Data)
  addSubCommand(exportTable1Data)
  addSubCommand(updateTable1Data)
  addSubCommand(generateTable1Tables)

  addSubCommand(auditTable2)
}
