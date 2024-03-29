package yhsb.app.cjb

import yhsb.base.command._
import yhsb.base.datetime.Formatter.toDashedDate
import yhsb.base.excel.Excel._
import yhsb.base.io.Path._
import yhsb.base.text.String.StringOps
import yhsb.base.util.UtilOps
import yhsb.cjb.db._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.{
  JBKind,
  JoinAuditQuery,
  WorkingPersonPauseAuditDetailQuery,
  WorkingPersonPauseAuditQuery,
  RetiredPersonPauseAuditDetailQuery,
  RetiredPersonPauseAuditQuery,
  RetiredPersonStopAuditQuery,
}
import yhsb.cjb.net.protocol.SessionOps.PersonStopAuditQuery

import scala.collection.mutable

class Audit(args: collection.Seq[String]) extends Command(args) {
  banner("城居保数据审核程序")

  addSubCommand(new JoinAudit)
  addSubCommand(new QueryAudit)
}

class JoinAudit extends Subcommand("join") with DateRange with Export {
  descr("参保审核与参保身份变更程序")

  val outputDir = """D:\特殊缴费"""
  val tmplXls = """批量信息变更模板.xls"""

  override def execute(): Unit = {
    val startDate = this.startDate.map(toDashedDate(_)).getOrElse("")
    val endDate = this.endDate.map(toDashedDate(_)).getOrElse("")
    val timeSpan = if (startDate != "") {
      if (endDate != "") {
        s"${startDate}_$endDate"
      } else {
        startDate
      }
    }
    println(timeSpan)

    val result = Session.use() { session =>
      session.request(JoinAuditQuery(startDate, endDate, auditState = "1"))
    }
    println(s"共计 ${result.size} 条")

    if (result.nonEmpty) {
      case class Item(idCard: String, name: String, jbKind: String)
      val items = mutable.ListBuffer[Item]()

      import AuthData2021._

      for (item <- result) {
        val message =
          s"${item.idCard} ${item.name.padRight(6)} ${item.birthDay}"
        val data: List[AuthItem] = run(
          historyData.filter(_.idCard == lift(item.idCard))
        )
        data.headOption match {
          case Some(v) =>
            println(
              s"$message ${v.jbKind.getOrElse("")} " +
                s"${if (v.name.get != item.name) v.name.get else ""}"
            )
            items.addOne(Item(item.idCard, item.name, v.jbKind.getOrElse("")))
          case None => println(message)
        }
      }

      if (export() && items.nonEmpty) {
        val workbook = Excel.load(outputDir / tmplXls)
        val sheet = workbook.getSheetAt(0)
        var index, copyIndex = 1

        items.foreach { item =>
          sheet.getOrCopyRow(index, copyIndex, clearValue = false).let { row =>
            row("B").value = item.idCard
            row("E").value = item.name
            row("J").value = JBKind.invert.getOrElse(item.jbKind, "")
            index += 1
          }
        }

        println(s"导出 批量信息变更$timeSpan.xls")
        workbook.save(outputDir / s"批量信息变更$timeSpan.xls")
      }
    }
  }
}

class QueryAudit extends Subcommand("query") {
  descr("居保审核查询程序")

  val operator = trailArg[String](
    descr = "经办人名称, 默认: wsjb, _: 为所有经办人",
    required = false,
    default = Option("wsjb")
  )

  val startAuditDate = opt[String](
    descr = "开始审核时间",
    default = Some("")
  )

  val endAuditDate = opt[String](
    descr = "结束审核时间",
    default = Some("")
  )

  case class AuditData(state: String, startDate: String, endDate: String)

  lazy val auditData = {
    AuditData(
      if (startAuditDate().isNullOrEmpty && endAuditDate().isNullOrEmpty) "0"
      else "1",
      if (startAuditDate().isNullOrEmpty) ""
      else toDashedDate(startAuditDate()),
      if (endAuditDate().isNullOrEmpty) "" else toDashedDate(endAuditDate())
    )
  }

  override def execute(): Unit = {
    Session.use() { session =>
      val operator_ = operator().let { op =>
        if (op == "_") "" else op
      }
      println(s"查询经办人: ${if (operator_.isEmpty) "所有" else operator_}")

      import auditData._

      println(" 参保审核查询 ".bar(60, '='))
      session
        .request(
          JoinAuditQuery(
            operator = operator_,
            auditState = state,
            startAuditDate = startDate,
            endAuditDate = endDate
          )
        )
        .zipWithIndex
        .foreach { case (item, i) =>
          println(s"${(i + 1).toString.padRight(3)} ${item.name
            .padRight(6)} ${item.idCard} ${item.opTime} ${item.operator}")
        }

      println(" 缴费人员终止查询 ".bar(60, '='))
      session
        .payingPersonStopAuditQuery(
          operator = operator_,
          auditState = state,
          startAuditDate = startDate,
          endAuditDate = endDate
        )
        .zipWithIndex
        .foreach { case (item, i) =>
          println(s"${(i + 1).toString.padRight(3)} ${item.name
            .padRight(6)} ${item.idCard} ${item.opTime} ${item.operator}")
        }

      println(" 待遇人员终止查询 ".bar(60, '='))
      session
        .retiredPersonStopAuditQuery(
          operator = operator_,
          auditState = state,
          startAuditDate = startDate,
          endAuditDate = endDate
        )
        .zipWithIndex
        .foreach { case (item, i) =>
          println(s"${(i + 1).toString.padRight(3)} ${item.name
            .padRight(6)} ${item.idCard} ${item.opTime} ${item.operator}")
        }

      println(" 缴费人员暂停查询 ".bar(60, '='))
      session
        .request(
          WorkingPersonPauseAuditQuery(
            auditState = state,
            startAuditDate = startDate,
            endAuditDate = endDate
          )
        )
        .flatMap { item =>
          session
            .request(WorkingPersonPauseAuditDetailQuery(item))
            .headOption match {
            case Some(v) if operator_ == "" || operator_ == v.operator =>
              Some((v.operator, item))
            case _ =>
              None
          }
        }
        .zipWithIndex
        .foreach { case ((oper, item), i) =>
          println(s"${(i + 1).toString.padRight(3)} ${item.name
            .padRight(6)} ${item.idCard} ${item.opTime} $oper")
        }

      println(" 待遇人员暂停查询 ".bar(60, '='))
      session
        .request(
          RetiredPersonPauseAuditQuery(
            auditState = state,
            startAuditDate = startDate,
            endAuditDate = endDate
          )
        )
        .flatMap { item =>
          session
            .request(RetiredPersonPauseAuditDetailQuery(item))
            .headOption match {
            case Some(v) if operator_ == "" || operator_ == v.operator =>
              Some((v.operator, item))
            case _ =>
              None
          }
        }
        .zipWithIndex
        .foreach { case ((oper, item), i) =>
          println(s"${(i + 1).toString.padRight(3)} ${item.name
            .padRight(6)} ${item.idCard} ${item.opTime} $oper")
        }
    }
  }
}

object Main {
  def main(args: Array[String]) = new Audit(args).runCommand()
}
