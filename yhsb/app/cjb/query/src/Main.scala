package yhsb.app.cjb.query

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.util.Optional._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbxxQuery
import yhsb.cjb.net.protocol.Cbxx
import yhsb.cjb.net.protocol.Jfxx
import yhsb.util.Files.appendToFileName
import yhsb.cjb.net.Result
import scala.collection.mutable
import yhsb.cjb.net.protocol.JfxxQuery
import yhsb.util.Strings._
import java.nio.file.Path

class Query(args: Seq[String]) extends Command(args) {

  banner("数据查询处理程序")

  val doc =
    new Subcommand("doc") with InputFile {
      descr("档案目录生成")

      def execute() = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- 0 to sheet.getLastRowNum()) {
            val row = sheet.getRow(i)
            val idcard = row.getCell("A").value
            val title = row.getCell("D").value

            session.sendService(CbxxQuery(idcard))
            val result = session.getResult[Cbxx]()
            if (result.isEmpty || result(0).idcard == null) {
              System.err.println(s"Error: ${i + 1} $idcard")
              System.exit(-1)
            } else {
              val cbxx = result(0)
              println(s"${i + 1} ${cbxx.name}")

              row
                .getOrCreateCell("E")
                .setCellValue(
                  s"${cbxx.name}$title"
                )
            }
          }
        }
        workbook.save(appendToFileName(inputFile(), ".upd"))
      }
    }

  val up =
    new Subcommand("up") with InputFile with RowRange {
      descr("更新参保信息")

      val nameRow = trailArg[String](descr = "姓名列名称")
      val idcardRow = trailArg[String](descr = "身份证列名称")
      val updateRow = trailArg[String](descr = "更新列名称")
      val xzjRow = opt[String](name = "xzj", short = 'x', descr = "更新乡镇街列名称")
      val mzbdRow = opt[String](name = "mzbd", short = 'm', descr = "更新姓名比对列名称")

      def execute(): Unit = {
        val workbook = Excel.load(inputFile())
        val sheet = workbook.getSheetAt(0)

        Session.use() { session =>
          for (i <- (startRow() - 1) until endRow()) {
            val row = sheet.getRow(i)
            val name = row.getCell(nameRow()).value.trim()
            val idcard = row.getCell(idcardRow()).value.trim().toUpperCase()

            println(idcard)

            session.sendService(CbxxQuery(idcard))
            //println(session.readBody())
            val result = session.getResult[Cbxx]()
            result.map(cbxx => {
              row
                .getOrCreateCell(updateRow())
                .setCellValue(cbxx.jbState)
              if (xzjRow.isDefined) {
                row
                  .getOrCreateCell(xzjRow())
                  .setCellValue(cbxx.dwName.get)
              }
              if (mzbdRow.isDefined && cbxx.name != name) {
                row
                  .getOrCreateCell(mzbdRow())
                  .setCellValue(cbxx.name)
              }
            })
          }
        }
        workbook.save(appendToFileName(inputFile(), ".upd"))
      }
    }

  val jfxx =
    new Subcommand("jfxx") with Export {
      descr("缴费信息查询")

      val idcard = trailArg[String](descr = "身份证号码")

      class JfxxRecord(
        val year: Int,
        var grjf: Option[BigDecimal] = None,
        var sjbt: Option[BigDecimal] = None,
        var sqbt: Option[BigDecimal] = None,
        var xjbt: Option[BigDecimal] = None,
        var zfdj: Option[BigDecimal] = None,
        var jtbz: Option[BigDecimal] = None,
        val hbrq: mutable.Set[String] = mutable.Set(),
        val sbjg: mutable.Set[String] = mutable.Set(),
      )

      class JfxxTotalRecord(
        grjf: Option[BigDecimal] = None,
        sjbt: Option[BigDecimal] = None,
        sqbt: Option[BigDecimal] = None,
        xjbt: Option[BigDecimal] = None,
        zfdj: Option[BigDecimal] = None,
        jtbz: Option[BigDecimal] = None,
        var total: Option[BigDecimal] = None
      ) extends JfxxRecord(
        0, grjf, sjbt, sqbt, xjbt, zfdj, jtbz
      )

      private implicit class OptionBigDecimalOps(left: Option[BigDecimal]) {
        def +(right: BigDecimal): Option[BigDecimal] = 
          if (left.isDefined) {
            Some(left.get + right)
          } else {
            Some(right)
          }

        def +(right: Option[BigDecimal]): Option[BigDecimal] =
          if (left.isDefined) {
            if (right.isDefined) Some(left.get + right.get)
            else left
          } else {
            right
          }

        def mkString = left match {
            case Some(value) => value.toString()
            case None => "0"
          }

        def padLeft(width: Int) = mkString.padLeft(width)

        def padRight(width: Int) = mkString.padRight(width)
      }

      def getJfxxRecords(
        jfxx: Result[Jfxx],
        payedRecords: mutable.Map[Int, JfxxRecord],
        unpayedRecords: mutable.Map[Int, JfxxRecord]
      ) = {
        for (data <- jfxx) {
          if (data.year != 0) {
            var records = if (data.isPayedOff) payedRecords else unpayedRecords
            if (!records.contains(data.year)) {
              records(data.year) = new JfxxRecord(data.year)
            }
            val record = records(data.year)
            data.item.value match {
              case "1" => record.grjf += data.amount
              case "3" => record.sjbt += data.amount
              case "4" => record.sqbt += data.amount
              case "5" => record.xjbt += data.amount
              case "6" => record.jtbz += data.amount
              case "11" => record.zfdj += data.amount
              case _ => println(s"未知缴费类型${data.item.value}, 金额${data.amount}")
            }
            record.sbjg.add(data.agency ?: "")
            record.hbrq.add(data.payedOffDay ?: "")
          }
        }
      }

      def orderAndTotal(records: collection.Map[Int, JfxxRecord]) = {
        var results = mutable.ListBuffer.from(records.values)
        results = results.sortWith((a, b) => a.year < b.year)
        val total = new JfxxTotalRecord
        for (r <- results) {
          total.grjf += r.grjf
          total.sjbt += r.sjbt
          total.sqbt += r.sqbt
          total.xjbt += r.xjbt
          total.zfdj += r.zfdj
          total.jtbz += r.jtbz
        }
        total.total = 
          total.grjf + total.sjbt + total.sqbt
          total.xjbt + total.zfdj + total.jtbz
        results.addOne(total)
      }

      def printInfo(info: Cbxx) = {
        println("个人信息:")
        println(
            s"${info.name} ${info.idcard} ${info.jbState} " +
            s"${info.jbKind} ${info.agency} ${info.czName} " +
            s"${info.opTime}\n"
        );
      }

      def printJfxxRecords(
        records: collection.Seq[JfxxRecord],
        message: String
      ) {
        println(message)
        println(
          s"${"序号".padLeft(4)}${"年度".padLeft(5)}${"个人缴费".padLeft(10)}" +
          s"${"省级补贴".padLeft(9)}${"市级补贴".padLeft(9)}${"县级补贴".padLeft(9)}" +
          s"${"政府代缴".padLeft(9)}${"集体补助".padLeft(9)}  社保经办机构 划拨时间"
        )

        def format(r: JfxxRecord) = {  
          r match {
            case t: JfxxTotalRecord =>
              s"合计${r.grjf.padLeft(9)}${r.sjbt.padLeft(9)}${r.sqbt.padLeft(9)}" +
              s"${r.xjbt.padLeft(9)}${r.zfdj.padLeft(9)}${r.jtbz.padLeft(9)}   " +
              (s"总计: ${t.total.getOrElse(0)}").padLeft(9)
            case _ =>
              s"${r.year.toString().padLeft(4)}${r.grjf.padLeft(9)}${r.sjbt.padLeft(9)}" +
              s"${r.sqbt.padLeft(9)}${r.xjbt.padLeft(9)}${r.zfdj.padLeft(9)}" +
              s"${r.jtbz.padLeft(9)}   ${r.sbjg.mkString("|")} ${r.hbrq.mkString("|")}"
          }
        }

        var i = 1
        for (r <- records) {
          r match {
            case t: JfxxTotalRecord =>
              println(s"     ${format(r)}")
            case _ =>
              println(s"${i.toString.padLeft(3)}  ${format(r)}")
              i += 1
          }
        }
      }

      override def execute(): Unit = {
        val (info, jfxx) = Session.use() { sess =>
          sess.sendService(CbxxQuery(idcard()))
          val cbxxResult = sess.getResult[Cbxx]()
          val info = if (cbxxResult.isEmpty || cbxxResult(0).invalid) {
            null
          } else {
            cbxxResult(0)
          }

          sess.sendService(JfxxQuery(idcard()))
          val jfxxResult = sess.getResult[Jfxx]()
          val jfxx = if (jfxxResult.isEmpty || 
            (jfxxResult.size == 1 && jfxxResult(0).year == 0)) {
            null
          } else {
            jfxxResult
          }

          (info, jfxx)
        }

        if (info == null) {
          println("未查到参保记录")
          return
        }

        printInfo(info)

        val (records, unrecords) = if (jfxx == null) {
          println("未查询到缴费信息")
          (null, null)
        } else {
          val payedRecords = mutable.Map[Int, JfxxRecord]()
          val unpayedRecords = mutable.Map[Int, JfxxRecord]()

          getJfxxRecords(jfxx, payedRecords, unpayedRecords)

          val records = orderAndTotal(payedRecords)
          val unrecords = orderAndTotal(unpayedRecords)

          printJfxxRecords(records, "已拨付缴费历史记录:")
          if (unpayedRecords.size > 0) {
            printJfxxRecords(unrecords, "\n未拨付补录入记录:")
          }

          (records, unrecords)
        }

        if (export()) {
          val path = """D:\征缴管理"""
          val xlsx = s"${path}\\雨湖区城乡居民基本养老保险缴费查询单模板.xlsx"
          val workbook = Excel.load(xlsx)
          val sheet = workbook.getSheetAt(0)
          sheet.getCell("A5").setCellValue(info.name)
          sheet.getCell("C5").setCellValue(info.idcard)
          sheet.getCell("E5").setCellValue(info.agency)
          sheet.getCell("G5").setCellValue(info.czName)
          sheet.getCell("K5").setCellValue(info.opTime)

          if (records != null) {
            var startRow, currentRow = 8
            for (r <- records) {
              val row = sheet.getOrCopyRow(currentRow, startRow)
              currentRow += 1

              row.getCell("A").setCellValue(
                r match {
                  case _: JfxxTotalRecord => ""
                  case _ => s"${currentRow - startRow}"
                }
              )

              row.getCell("B").setCellValue(
                r match {
                  case _: JfxxTotalRecord => "合计"
                  case _ => s"${r.year}"
                }
              )

              row.getCell("C").setCellValue(r.grjf.mkString)
              row.getCell("D").setCellValue(r.sjbt.mkString)
              row.getCell("E").setCellValue(r.sqbt.mkString)
              row.getCell("F").setCellValue(r.xjbt.mkString)
              row.getCell("G").setCellValue(r.zfdj.mkString)
              row.getCell("H").setCellValue(r.jtbz.mkString)
              row.getCell("I").setCellValue(
                r match {
                  case _: JfxxTotalRecord => "总计"
                  case _ => r.sbjg.mkString("|")
                }
              )
              row.getCell("K").setCellValue(
                r match {
                  case t: JfxxTotalRecord => t.total.mkString
                  case _ => r.hbrq.mkString("|")
                }
              )
              workbook.save(Path.of(path, s"${info.name}缴费查询单.xlsx"))
            }
          }
        }

      }
    }

  addSubCommand(doc)
  addSubCommand(up)
  addSubCommand(jfxx)
}

object Main {
  def main(args: Array[String]) = new Query(args).runCommand()
}
