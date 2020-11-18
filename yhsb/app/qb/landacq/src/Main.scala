package yhsb.app.qb.landacq

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbxxRequest
import yhsb.cjb.net.protocol.Cbxx
import yhsb.util.Files.appendToFileName
import java.nio.file.Paths
import scala.util.matching.Regex
import scala.collection.mutable
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.FileInputStream
import scala.collection.JavaConverters._

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
                sess.sendService(CbxxRequest(idcard))
                val result = sess.getResult[Cbxx]()
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

        workbook.save(appendToFileName(inputFile(), ".up"))
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
          def setIntOption(colName: String, value: Option[Int]) = {
            if (value.isDefined) row(colName).setCellValue(value.get)
          }
          def setBigDecOption(colName: String, value: Option[BigDecimal]) = {
            if (value.isDefined) row(colName).setCellValue(value.get.toString())
          }
          val data = map(idcard)
          setIntOption("H", data.totalYears)
          setIntOption("I", data.subsidyYears)
          setBigDecOption("J", data.totalMoney)
          setBigDecOption("K", data.subsidyMoney)
          setBigDecOption("L", data.paidMoney)
          setBigDecOption("M", data.defference)
        }
      }
      workbook.save(appendToFileName(updateXls(), ".up"))
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
          def cellValue2Option[T](
              colName: String,
              getValue: Cell => String = _.value
          )(convert: String => T): Option[T] = {
            val value = getValue(row(colName))
            try {
              if (value != null && !value.isEmpty()) Some(convert(value))
              else None
            } catch {
              case _: Exception => None
            }
          }
          val totalYears = cellValue2Option(totalYearsCol())(_.toInt)
          val subsidyYears = cellValue2Option(subsidyYearsCol())(_.toInt)
          val totalMoney = cellValue2Option(totalMoneyCol())(BigDecimal(_))
          val subsidyMoney = cellValue2Option(subsidyMoneyCol())(BigDecimal(_))
          val paidMoney = cellValue2Option(paidMoneyCol())(BigDecimal(_))
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

  val zhbj = new Subcommand("zhbj") with InputFile {
    descr("转换补缴申请表")

    val template = """D:\征地农民\雨湖区被征地农民一次性补缴养老保险申请表模板.xlsx"""

    def execute(): Unit = {
      val doc = new XWPFDocument(new FileInputStream(inputFile()))
      val workbook = Excel.load(template)
      val sheet = workbook.getSheetAt(0)

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
          println()
          currentRow += 1
        }
      }

      workbook.save(s"${inputFile()}.conv.xlsx")
    }
  }

  addSubCommand(dump)
  addSubCommand(jbstate)
  addSubCommand(upsub)
  addSubCommand(zhbj)
}

object Main {
  def main(args: Array[String]) = new LandAcq(args).runCommand()
}
