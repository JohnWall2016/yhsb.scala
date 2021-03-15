package yhsb.app.qb.landacq

import yhsb.base.command._
import yhsb.base.excel.Excel
import yhsb.base.excel.Excel._
import yhsb.cjb.net.Session
import java.nio.file.Paths
import scala.util.matching.Regex
import scala.collection.mutable
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileInputStream
import scala.collection.JavaConverters._
import yhsb.cjb.net.protocol.PersonInfoInProvinceQuery
import yhsb.base.text.Strings._

class LandAcq(args: Seq[String]) extends Command(args) {

  banner("征地农民处理程序")

  val dump = new Subcommand("dump") with InputFile {
    descr("导出特定单位数据")

    val outDir = """D:\征地农民"""
    val template = "雨湖区被征地农民数据模板.xlsx"

    val dwName = trailArg[String](descr = "导出单位名称")

    val filter =
      opt[String](required = false, descr = "过滤单位名称")

    def execute() = {

      val inWorkbook = Excel.load(inputFile())
      val inSheet = inWorkbook.getSheetAt(1)

      val outWorkbook = Excel.load(Paths.get(outDir, template))
      val outSheet = outWorkbook.getSheetAt(0)

      var index, copyIndex = 1

      val regex = if (filter.isDefined) filter() else dwName()
      val mat = new Regex(s".*((万楼)|(护潭)).*") //new Regex(s".*$regex.*")

      for (i <- 3 to inSheet.getLastRowNum()) {
        val inRow = inSheet.getRow(i)
        val xcz = inRow.getCell("U").value

        println(s"$i $xcz")

        if (mat.matches(xcz)) {
          val no = inRow.getCell("A").value
          val project = inRow.getCell("D").value
          val name = inRow.getCell("F").value
          var idcard = inRow.getCell("J").value

          idcard = idcard.trim().toUpperCase()
          val sex = if (idcard.length() == 18) {
            val c = idcard.substring(16, 17)
            if (c.toInt % 2 == 1) "男" else "女"
          } else ""

          val memo = xcz

          val outRow = outSheet.getOrCopyRow(index, copyIndex, false)
          outRow.getCell("A").setCellValue(index)
          outRow.getCell("B").setCellValue(no)
          outRow.getCell("C").setCellValue(project)
          outRow.getCell("D").setCellValue(name)
          outRow.getCell("E").setCellValue(sex)
          outRow.getCell("F").setCellValue(idcard)
          outRow.getCell("G").setCellValue(memo)

          index += 1
        }
      }

      outWorkbook.save(Paths.get(outDir, s"${dwName()}被征地农民数据.xlsx"))
    }
  }

  val jbstate =
    new Subcommand("jbstate") with InputFile with SheetIndexOpt with RowRange {
      descr("查询保存居保状态")

      val nameCol = trailArg[String](descr = "姓名所在列")

      val idcardCol = trailArg[String](descr = "身份号码所在列")

      val resultCol = trailArg[String](descr = "居保状态回写列")

      val retireTime = trailArg[Int](descr = "退休时间")

      def execute() = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(sheetIndex())

        Session.use() { sess =>
          for (i <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(i)
            val name = row.getCell(nameCol()).value.trim()
            val idcard = row.getCell(idcardCol()).value.trim().toUpperCase()

            if (idcard.length() == 18) {
              val male = (idcard.substring(16, 17).toInt % 2) == 1
              val birthMonth = idcard.substring(6, 12).toInt

              println(s"$name $idcard $male $birthMonth")

              if (
                (male && (birthMonth + 6000 <= retireTime()))
                || (!male && (birthMonth + 5500 <= retireTime()))
              ) {
                sess.sendService(PersonInfoInProvinceQuery(idcard))
                val result = sess.getResult[PersonInfoInProvinceQuery#Item]()
                val jbState = {
                  val cbxx = result(0)
                  if (name == cbxx.name || cbxx.name == null) cbxx.jbState
                  else s"${cbxx.jbState}(${cbxx.name})"
                }
                println(jbState)
                row
                  .getOrCreateCell(resultCol())
                  .setCellValue(jbState)
              }
            }
          }
        }

        workbook.save(inputFile().insertBeforeLast(".up"))
      }
    }

  val upsub = new Subcommand("upsub") {
    descr("更新政府补贴数据")

    val baseXls = trailArg[String](descr = "数据来源excel文件")
    def baseSheetIndex() = 2
    def nameCol() = "D"
    def idcardCol() = "I"
    def totalYearsCol() = "K"
    def subsidyYearsCol() = "L"
    def totalMoneyCol() = "M"
    def subsidyMoneyCol() = "N"
    def paidMoneyCol() = "O"

    val updateXls = trailArg[String](descr = "数据更新excel文件")

    def execute(): Unit = {
      val map = loadBase()

      val workbook = Excel.load(updateXls())
      val sheet = workbook.getSheetAt(0)

      for (row <- sheet.rowIterator(1)) {
        val idcard = row("G").value.trim().toUpperCase
        println(s"更新 $idcard")
        if (map.contains(idcard)) {
          val data = map(idcard)
          row.setCellValue("H", data.totalYears)
          row.setCellValue("I", data.subsidyYears)
          row.setCellValue("J", data.totalMoney)
          row.setCellValue("K", data.subsidyMoney)
          row.setCellValue("L", data.paidMoney)
          row.setCellValue("M", data.defference)
        }
      }
      workbook.save(updateXls().insertBeforeLast(".up"))
    }

    private case class Data(
        name: String,
        idcard: String,
        totalYears: Option[Int],
        subsidyYears: Option[Int],
        totalMoney: Option[BigDecimal],
        subsidyMoney: Option[BigDecimal],
        paidMoney: Option[BigDecimal],
        defference: Option[BigDecimal]
    )

    private def loadBase(): mutable.Map[String, Data] = {
      val workbook = Excel.load(baseXls())
      val sheet = workbook.getSheetAt(baseSheetIndex())

      val map = mutable.Map[String, Data]()

      for (row <- sheet.rowIterator(1)) {
        val name = row(nameCol()).value.trim()
        val idcard = row(idcardCol()).value.trim().toUpperCase()
        //println(s"$name $idcard")
        if (map.contains(idcard)) {
          println(s"$name $idcard 身份证重复")
        } else {
          val totalYears = row.cellValue(totalYearsCol())(_.toInt)
          val subsidyYears = row.cellValue(subsidyYearsCol())(_.toInt)
          val totalMoney = row.cellValue(totalMoneyCol())(BigDecimal(_))
          val subsidyMoney = row.cellValue(subsidyMoneyCol())(BigDecimal(_))
          val paidMoney = row.cellValue(paidMoneyCol())(BigDecimal(_))
          val defference =
            if (
              subsidyMoney.isDefined &&
              subsidyYears.isDefined &&
              subsidyYears.get != 0 &&
              subsidyYears.get <= 15
            )
              Some(
                (7279.2 - (subsidyMoney.get / subsidyYears.get)) * subsidyYears.get
              )
            else None

          val data = Data(
            name,
            idcard,
            totalYears,
            subsidyYears,
            totalMoney,
            subsidyMoney,
            paidMoney,
            defference
          )
          map(idcard) = data
          //println(data)
        }
      }

      map
    }
  }

  case class Data(
    btje: Option[BigDecimal], // 补贴金额
    xmmc: Option[String],     // 项目名称
    daxh: Option[Int],        // ??序号
    age: Option[Int],      // 年龄
    bjnx: Option[Int],     // 补缴年限
    hjje: Option[BigDecimal], // 合计金额
    grjf: Option[BigDecimal], // 个人缴费
  )

  def loadId2BtMap(): collection.Map[String, Data] = {
    def baseXls() = """D:\征地农民\3文件总表2016.3.18.xls"""//"""D:\Downloads\三文件总表2016.3.18.xls"""
    def baseSheetIndex() = 5
    def nameCol() = "D"
    def idcardCol() = "I"
    def btCol() = "N"
    def daxhCol() = "C"
    def xmmcCols() = List("T", "S", "R")
    def ageCols() = List("F", "G")
    def bjnxCol() = "K"
    def hjjeCol() = "M"
    def grjfCol() = "O"

    val workbook = Excel.load(baseXls())
    val sheet = workbook.getSheetAt(baseSheetIndex())
    println(sheet.getSheetName())

    val map = mutable.Map[String, Data]()

    for (row <- sheet.rowIterator(1)) {

      val name = row(nameCol()).value.trim()
      val idcard = row(idcardCol()).value.trim().toUpperCase()
      //println(s"$name $idcard")
      //println(s"${row.getRowNum()} ${idcard} ${name}")
      if (map.contains(idcard)) {
        println(s"$name $idcard 身份证重复")
      } else {
        var xmmc: Option[String] = None
        for (col <- xmmcCols() if xmmc.isEmpty) {
          val v = row.getCell(col).value
          xmmc = if (v.length() > 5) Some(v) else None
        }

        var age: Option[Int] = None
        for (col <- ageCols() if age.isEmpty) {
          val v = row.getCell(col).value.trim()
          age = if (v.length == 2) Some(v.toInt) else None
        }

        map(idcard) = Data(
          row.cellValue(btCol())(BigDecimal(_)),
          xmmc,
          row.cellValue(daxhCol())(_.toInt),
          age,
          row.cellValue(bjnxCol())(_.toInt),
          row.cellValue(hjjeCol())(BigDecimal(_)),
          row.cellValue(grjfCol())(BigDecimal(_)),
        )
      }
    }
    map
  }

  val zhbj = new Subcommand("zhbj") with InputFile {
    descr("转换补缴申请表")

    val template = """D:\征地农民\雨湖区被征地农民一次性补缴养老保险申请表模板.xlsx"""

    def execute(): Unit = {
      val doc = new XWPFDocument(new FileInputStream(inputFile()))
      val workbook = Excel.load(template)
      val sheet = workbook.getSheetAt(0)

      val map = loadId2BtMap()

      for (par <- doc.getParagraphs().asScala) {
        for {
          runs <- par.getRuns().asScala
          runsText = runs.text() if runsText.matches("项目名称：.*")
        } {
          println(runsText)
          sheet.getCell("A2").setCellValue(runsText)
        }
      }

      var startRow, currentRow = 4

      for (table <- doc.getTables().asScala) {
        for (row <- table.getRows().asScala.drop(2)) {
          val r = sheet.getOrCopyRow(currentRow, startRow)
          r.setHeight(row.getHeight().toShort)
          for ((cell, index) <- row.getTableCells().asScala.zipWithIndex) {
            print(s"$index ${cell.getText()} ")
            r.getCell(index).setCellValue(cell.getText())
          }
          map
            .get(r.getCell("H").value)
            .map(_.btje.map(v => r.getCell("L").setCellValue(v.toDouble)))
          println()
          currentRow += 1
        }
      }

      workbook.save(s"${inputFile()}.conv.xlsx")
    }
  }

  val check = new Subcommand("check") with InputFile {
    descr("查询是否在三文件总表中")

    val sheetIndexes =
      trailArg[List[Int]](
        descr = "数据表序号, 默认为0",
        default = Some(List(0)),
        required = false
      )

    def execute(): Unit = {
      val map = loadId2BtMap()

      val workbook = Excel.load(inputFile())

      for (i <- sheetIndexes()) {
        val sheet = workbook.getSheetAt(i)

        for (row <- sheet.rowIterator(3)) {
          val idcard = row.getCell("H").value.trim.toUpperCase()
          if (idcard != "") {
            println(idcard)
            if (map.contains(idcard)) {
              row
                .getOrCreateCell("M")
                .setCellValue("有")
              map(idcard).btje.map { bt =>
                row
                  .getOrCreateCell("N")
                  .setCellValue(bt.toDouble)
              }
            } else {
              row
                .getOrCreateCell("M")
                .setCellValue("无")
            }
          }
        }
      }

      workbook.save(inputFile().insertBeforeLast(".upd"))
    }
  }

  val fetch = new Subcommand("fetch") with InputFile {
    descr("获取三文件总表中数据")

    val sheetIndexes =
      trailArg[List[Int]](
        descr = "数据表序号, 默认为0",
        default = Some(List(0)),
        required = false
      )

    def execute(): Unit = {
      val map = loadId2BtMap()

      println(inputFile())
      val workbook = Excel.load(inputFile())

      for (i <- sheetIndexes()) {
        println(i)
        val sheet = workbook.getSheetAt(i)
        println(sheet.getSheetName())
        println(sheet.getLastRowNum())

        for (row <- sheet.rowIterator(3)) {
          val idcard = row.getCell("J").value.trim.toUpperCase()
          if (idcard != "") {
            println(idcard)
            if (map.contains(idcard)) {
              val data = map(idcard)
              /*data.btje.map { bt =>
                row
                  .getOrCreateCell("L")
                  .setCellValue(bt.toDouble)
              }
              data.daxh.map { xh =>
                row
                  .getOrCreateCell("N")
                  .setCellValue(xh)
              }
              data.xmmc.map { xm =>
                row
                  .getOrCreateCell("M")
                  .setCellValue(xm)
              }*/
              data.age.map { age =>
                row
                  .getOrCreateCell("H")
                  .setCellValue(age)
              }
              data.bjnx.map { bjnx =>
                row
                  .getOrCreateCell("K")
                  .setCellValue(bjnx)
              }
              data.hjje.map { hjje =>
                row
                  .getOrCreateCell("M")
                  .setCellValue(hjje.toDouble)
              }
              data.grjf.map { grjf =>
                row
                  .getOrCreateCell("O")
                  .setCellValue(grjf.toDouble)
              }
            }
          }
        }
      }

      workbook.save(inputFile().insertBeforeLast(".upd"))
    }
  }


  addSubCommand(dump)
  addSubCommand(jbstate)
  addSubCommand(upsub)
  addSubCommand(zhbj)
  addSubCommand(check)
  addSubCommand(fetch)
}

object Main {
  def main(args: Array[String]) = new LandAcq(args).runCommand()
}
