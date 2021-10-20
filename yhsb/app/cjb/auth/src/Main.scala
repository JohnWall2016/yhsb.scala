package yhsb.app.cjb

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

import yhsb.cjb.db.RawItem
import yhsb.cjb.db.AuthData2021
import yhsb.cjb.db.AuthItem
import yhsb.cjb.db.HistoryItem
import yhsb.cjb.db.MonthItem
import yhsb.cjb.db.JoinedPersonData
import yhsb.cjb.net.protocol._
import yhsb.cjb.net.Session

object Main {
  def main(args: Array[String]): Unit = new Auth(args).runCommand()
}

class Auth(args: collection.Seq[String]) extends Command(args) {
  banner("城居参保身份认证程序")

  addSubCommand(new ImportVeryPoor)
  addSubCommand(new ImportCityAllowance)
  addSubCommand(new ImportCountryAllowance)
  addSubCommand(new ImportDisability)
  addSubCommand(new ImportOutOfPoverty)
  addSubCommand(new MergeHistory)
  addSubCommand(new GenerateBook)
  addSubCommand(new Authenticate)
  addSubCommand(new ExportJBData)
  addSubCommand(new ImportJBData)
  addSubCommand(new UpdateJBState)
  addSubCommand(new ExportData)
  addSubCommand(new ExportChangedData)
}

object Auth {
  def importRawData(items: Iterable[RawItem]) = {
    import AuthData2021._

    transaction {
      items.zipWithIndex
        .foreach { case (item, index) =>
          print(
            s"${index + 1} ${item.idCard} ${item.name.getOrElse("").padRight(6)} ${item.personType.getOrElse("")} "
          )
          if (item.idCard.nonNullAndEmpty) {
            val result: List[RawItem] =
              run(
                rawData.filter { it =>
                  it.idCard == lift(item.idCard) &&
                  it.personType == lift(item.personType) &&
                  it.date == lift(item.date)
                }
              )
            if (result.nonEmpty) {
              result.foreach { it =>
                item.no = it.no
                run(
                  rawData.filter(_.no == lift(item.no)).update(lift(item))
                )
              }
              println("更新")
            } else {
              run(
                rawData.insert(lift(item))
              )
              println("新增")
            }
          } else {
            println("数据无效")
          }
        }
    }
  }

  def mergeRawData(date: String, month: String = null) = {
    import AuthData2021._

    transaction {
      val list: List[RawItem] = run(
        rawData.filter(_.date == lift(Option(date)))
      )

      val groups = list.groupBy(_.idCard)

      if (month == null) {
        groups.zipWithIndex.foreach { case ((idCard, items), index) =>
          println(s"${index + 1} $idCard")

          val data: List[HistoryItem] =
            run(historyData.filter(_.idCard == lift(idCard)))
          if (data.nonEmpty) {
            data.foreach { item =>
              items.foreach(item.merge(_))
              run(historyData.filter(_.no == lift(item.no)).update(lift(item)))
            }
          } else {
            val item = HistoryItem()
            items.foreach(item.merge(_))
            run(historyData.insert(lift(item)))
          }
        }
      } else {
        groups.zipWithIndex.foreach { case ((idCard, items), index) =>
          println(s"${index + 1} $idCard")

          val data: List[MonthItem] =
            run(
              monthData.filter(it =>
                it.idCard == lift(idCard) && it.month == lift(Option(month))
              )
            )
          if (data.nonEmpty) {
            data.foreach { item =>
              items.foreach(item.merge(_))
              run(monthData.filter(_.no == lift(item.no)).update(lift(item)))
            }
          } else {
            val item = MonthItem(month = Some(month))
            items.foreach(item.merge(_))
            run(monthData.insert(lift(item)))
          }
        }
      }
    }
  }

  def authenticate(date: String, monthOrAll: String) = {
    import AuthData2021._

    transaction {
      val data: List[AuthItem] =
        if (monthOrAll.toUpperCase() == "ALL") {
          run(historyData)
        } else {
          run(monthData.filter(_.month == lift(Option(monthOrAll))))
        }

      var index = 1
      data.foreach { item =>
        var jbKind: String = null
        var isDestitute: String = null
        if (item.veryPoor.getOrElse("").nonEmpty) {
          jbKind = "特困一级"
          isDestitute = "特困人员"
        } else if (item.fullAllowance.getOrElse("").nonEmpty) {
          jbKind = "低保对象一级"
          isDestitute = "低保对象"
        } else if (item.primaryDisability.getOrElse("").nonEmpty) {
          jbKind = "残一级"
        } else if (item.shortAllowance.getOrElse("").nonEmpty) {
          jbKind = "低保对象二级"
          isDestitute = "低保对象"
        } else if (item.secondaryDisability.getOrElse("").nonEmpty) {
          jbKind = "残二级"
        } else if (item.poverty.getOrElse("").nonEmpty) {
          jbKind = "贫困人口一级"
        }

        var update = false
        if (jbKind != null) {
          item.jbKind match {
            case None =>
              println(
                s"$index ${item.idCard} ${item.name.getOrElse("").padRight(6)} $jbKind"
              )
              item.jbKind = Some(jbKind)
              item.jbKindFirstDate = Some(date)
              item.jbKindLastDate = None
              index += 1
              update = true
            case Some(value) =>
              if (jbKind != value) {
                println(
                  s"$index ${item.idCard} ${item.name.getOrElse("").padRight(6)} $jbKind <- ${item.jbKind}"
                )
                item.jbKind = Some(jbKind)
                if (item.jbKindFirstDate.isEmpty) {
                  item.jbKindFirstDate = Some(date)
                  item.jbKindLastDate = None
                } else {
                  item.jbKindLastDate = Some(date)
                }
                index += 1
                update = true
              } else if (item.jbKindFirstDate.isEmpty) {
                println(
                  s"$index ${item.idCard} ${item.name.getOrElse("").padRight(6)} $jbKind <- ${item.jbKind}"
                )
                item.jbKindFirstDate = Some(date)
                item.jbKindLastDate = None
                index += 1
                update = true
              }
          }
        }

        if (
          isDestitute != null && isDestitute != item.isDestitute.getOrElse("")
        ) {
          item.isDestitute = Some(isDestitute)
          update = true
        }

        if (update) {
          if (monthOrAll.toUpperCase() == "ALL") {
            val historyItem = item.asInstanceOf[HistoryItem]
            run(
              historyData
                .filter(_.no == lift(historyItem.no))
                .update(lift(historyItem))
            )
          } else {
            val monthItem = item.asInstanceOf[MonthItem]
            run(
              monthData
                .filter(_.no == lift(monthItem.no))
                .update(lift(monthItem))
            )
          }
        }
      }
    }
  }

  def exportData(monthOrAll: String, template: String, file: String) = {
    println(s"开始导出底册: $file")

    val workbook = Excel.load(template)
    val sheet = workbook.getSheetAt(0)
    val startRow = 2
    var currentRow = startRow

    import AuthData2021._

    val data: List[AuthItem] =
      if (monthOrAll.toUpperCase() == "ALL") {
        run {
          quote(
            infix"$historyData ORDER BY CONVERT( xzj USING gbk ), CONVERT( csq USING gbk ), CONVERT( name USING gbk )"
              .as[Query[HistoryItem]]
          )
        }
      } else {
        run {
          quote(
            infix"${monthData.filter(_.month == Option(lift(monthOrAll)))} ORDER BY CONVERT( xzj USING gbk ), CONVERT( csq USING gbk ), CONVERT( name USING gbk )"
              .as[Query[MonthItem]]
          )
        }
      }

    data.foreach { item =>
      val index = currentRow - startRow + 1

      println(s"$index ${item.idCard} ${item.name.getOrElse("")}")

      val row = sheet.getOrCopyRow(currentRow, startRow)
      currentRow += 1
      row("A").value = index
      row("B").value = item.no
      row("C").value = item.neighborhood
      row("D").value = item.community
      row("E").value = item.address
      row("F").value = item.name
      row("G").value = item.idCard
      row("H").value = item.birthDay
      row("I").value = item.veryPoor
      row("J").value = item.veryPoorDate
      row("K").value = item.fullAllowance
      row("L").value = item.fullAllowanceDate
      row("M").value = item.shortAllowance
      row("N").value = item.shortAllowanceDate
      row("O").value = item.primaryDisability
      row("P").value = item.primaryDisabilityDate
      row("Q").value = item.secondaryDisability
      row("R").value = item.secondaryDisabilityDate
      row("S").value = item.poverty
      row("T").value = item.povertyDate
      row("U").value = item.isDestitute
      row("V").value = item.jbKind
      row("W").value = item.jbKindFirstDate
      row("X").value = item.jbKindLastDate
      row("Y").value = item.jbState
      row("Z").value = item.jbStateDate
      row("AA").setBlank()
    }

    workbook.save(file)
    println(s"结束导出底册: $file")
  }
}

trait ImportCommand { _: ScallopConf =>
  val date = trailArg[String](descr = "数据月份, 例如: 201912")
  val excel = trailArg[String](descr = "excel表格文件路径")
  val startRow = trailArg[Int](descr = "开始行, 从1开始")
  val endRow = trailArg[Int](descr = "结束行, 包含在内")
  val clear = opt[Boolean](descr = "是否清除已有数据", default = Some(false))
  val clearOnly = opt[Boolean](descr = "只清除已有数据", default = Some(false))

  def clearData(): Unit

  /**
   * 提取数据字段信息
   *
   * @param nameAndIdCards 姓名 -> 身份证号码
   * @param neighborhood 乡镇、街道
   * @param community 村、社区
   * @param address 地址
   * @param personType 人员类型
   * @param init 初始化信息操作
   */
  case class FieldColumns(
      nameAndIdCards: Seq[(String, String)],
      neighborhood: String,
      community: String,
      address: Option[String] = None,
      personType: Option[String] = None
  )(
      val init: RawItem => Unit
  )

  val fieldColumns: FieldColumns

  private def fetchData(): Iterable[RawItem] = {
    require(
      startRow() > 0 && startRow() <= endRow(),
      "开始行必须大于0, 且不能大于结束行"
    )

    val workbook = Excel.load(excel())
    val sheet = workbook.getSheetAt(0)

    for {
      index <- (startRow() - 1) until endRow()
      row = sheet.getRow(index)
      neighborhood = row(fieldColumns.neighborhood).value
      community = row(fieldColumns.community).value
      personType = fieldColumns.personType.map(row(_).value).getOrElse("")
      address = fieldColumns.address.map(row(_).value).getOrElse("")
      (nameCol, idCardCol) <- fieldColumns.nameAndIdCards
      name = row(nameCol).value.trim()
      idCard = row(idCardCol).value.trim()
      len = idCard.length()
      if (name.nonEmpty && idCard.nonEmpty && len >= 18)
    } yield {
      val item = RawItem(
        neighborhood = Some(neighborhood),
        community = Some(community),
        address = Some(address),
        name = Some(name),
        idCard = if (len > 18) idCard.substring(0, 18) else idCard,
        birthDay = Some(idCard.substring(6, 14)),
        date = Some(date()),
        personType = Some(personType)
      )
      fieldColumns.init(item)
      item
    }
  }

  def execute(): Unit = {
    if (clear() || clearOnly()) {
      println("开始清除已有数据")
      clearData()
      println("结束清除已有数据")
    }
    if (!clearOnly()) {
      Auth.importRawData(fetchData())
    }
  }
}

class ImportVeryPoor extends Subcommand("tkry") with ImportCommand {
  descr("导入特困人员数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData
        .filter(it =>
          it.date == Option(lift(date())) && it.personType == Some("特困人员")
        )
        .delete
    )
  }

  override val fieldColumns: FieldColumns =
    FieldColumns(
      nameAndIdCards = Seq("G" -> "H"),
      neighborhood = "C",
      community = "D",
      address = Some("E")
    ) { item =>
      item.personType = Some("特困人员")
      item.detail = Some("是")
    }
}

class ImportCityAllowance extends Subcommand("csdb") with ImportCommand {
  descr("导入城市低保数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData
        .filter(it =>
          it.date == Option(lift(date())) &&
            (infix"${it.personType} like '%低保人员'".as[Boolean]) &&
            it.detail == Some("城市")
        )
        .delete
    )
  }

  override val fieldColumns: FieldColumns =
    FieldColumns(
      nameAndIdCards = Seq(
        "I" -> "J",
        "K" -> "L",
        "M" -> "N",
        "O" -> "P",
        "Q" -> "R"
      ),
      neighborhood = "A",
      community = "B",
      address = Some("E"),
      personType = Some("G")
    ) { item =>
      item.personType = item.personType match {
        case Some("全额救助") | Some("全额") => Some("全额低保人员")
        case None | Some(_)            => Some("差额低保人员")
      }
      item.detail = Some("城市")
    }
}

class ImportCountryAllowance extends Subcommand("ncdb") with ImportCommand {
  descr("导入农村低保数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData
        .filter(it =>
          it.date == Option(lift(date())) &&
            (infix"${it.personType} like '%低保人员'".as[Boolean]) &&
            it.detail == Some("农村")
        )
        .delete
    )
  }

  override val fieldColumns: FieldColumns =
    FieldColumns(
      nameAndIdCards = Seq(
        "H" -> "J",
        "K" -> "L",
        "M" -> "N",
        "O" -> "P",
        "Q" -> "R",
        "S" -> "T"
      ),
      neighborhood = "A",
      community = "B",
      address = Some("D"),
      personType = Some("F")
    ) { item =>
      item.personType = item.personType match {
        case Some("全额救助") | Some("全额") => Some("全额低保人员")
        case None | Some(_)            => Some("差额低保人员")
      }
      item.detail = Some("农村")
    }
}

class ImportDisability extends Subcommand("cjry") with ImportCommand {
  descr("导入残疾人员数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData
        .filter(it =>
          it.date == Option(lift(date())) &&
            (infix"${it.personType} like '%残疾人员'".as[Boolean])
        )
        .delete
    )
  }

  override val fieldColumns: FieldColumns =
    FieldColumns(
      nameAndIdCards = Seq(
        "A" -> "B"
      ),
      neighborhood = "E",
      community = "F",
      address = Some("G"),
      personType = Some("M")
    ) { item =>
      item.detail = item.personType
      item.personType = item.personType match {
        case Some("一级") | Some("二级") => Some("一二级残疾人员")
        case Some("三级") | Some("四级") => Some("三四级残疾人员")
        case other                   => throw new Exception(s"未知残疾类型: $other")
      }
    }
}

class ImportOutOfPoverty extends Subcommand("jzfp") with ImportCommand {
  descr("导入精准扶贫数据")

  override def clearData(): Unit = {
    import AuthData2021._
    run(
      rawData
        .filter(it =>
          it.date == Option(lift(date())) && it.personType == Some("精准扶贫")
        )
        .delete
    )
  }

  override val fieldColumns: FieldColumns =
    FieldColumns(
      nameAndIdCards = Seq("G" -> "H"),
      neighborhood = "C",
      community = "D",
    ) { item =>
      item.personType = Some("精准扶贫")
      item.detail = Some("脱贫户")
    }
}

class MergeHistory extends Subcommand("hbdc") {
  descr("合并历史数据底册")

  val date = trailArg[String](descr = "数据月份, 例如: 201912")

  override def execute(): Unit = {
    println("开始合并导入数据至: 特殊缴费类型历史数据底册")
    Auth.mergeRawData(date())
    println("结束合并导入数据至: 特殊缴费类型历史数据底册")
  }
}

class GenerateBook extends Subcommand("scdc") {
  descr("生成当月数据底册")

  val date = trailArg[String](descr = "数据月份, 例如: 201912")
  val clear = opt[Boolean](descr = "是否清除数据表")

  override def execute(): Unit = {
    import AuthData2021._

    if (clear()) {
      println(s"开始清除数据: ${date()}特殊缴费类型数据底册")
      run(monthData.filter(_.month == Some(lift(date()))).delete)
      println(s"结束清除数据: ${date()}特殊缴费类型数据底册")
    }

    println(s"开始合并导入数据至: ${date()}特殊缴费类型数据底册")
    Auth.mergeRawData(date(), date())
    println(s"结束合并导入数据至: ${date()}特殊缴费类型数据底册")
  }
}

class Authenticate extends Subcommand("rdsf") {
  descr("认定居保参保身份")

  val monthOrAll = trailArg[String](descr = "表格数据月份, 例如: 201912, ALL")
  val date = trailArg[String](descr = "认定月份, 例如: 201912")

  override def execute(): Unit = {
    println(s"开始认定参保人员身份: ${monthOrAll()}特殊缴费类型数据底册")
    Auth.authenticate(date(), monthOrAll())
    println(s"结束认定参保人员身份: ${monthOrAll()}特殊缴费类型数据底册")
  }
}

class ExportJBData extends Subcommand("dcjb") {
  descr("导出居保参保数据")

  val outputDir = """D:\特殊缴费\参保人员明细表"""

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
    import AuthData2021._

    if (clear()) {
      println("开始清除数据: 居保参保人员明细表")
      run(joinedPersonData.delete)
      println("结束清除数据: 居保参保人员明细表")
    }

    println("开始导入居保参保人员明细表")

    AuthData2021.loadExcel(
      joinedPersonData.quoted,
      excel(),
      startRow(),
      endRow(),
      Seq("F", "A", "B", "D", "G", "H", "J", "L", "M", "R"),
      printSql = true
    )

    println("结束导入居保参保人员明细表")
  }
}

class UpdateJBState extends Subcommand("jbzt") {
  descr("更新居保参保状态")

  val monthOrAll = trailArg[String](descr = "表格数据月份, 例如: 201912, ALL")
  val date = trailArg[String](descr = "居保数据日期, 例如: 20191231")

  override def execute(): Unit = {
    import AuthData2021._

    println(s"开始更新居保状态: ${monthOrAll()}特殊缴费类型数据底册")

    transaction {
      val peopleTable = "jbrymx"
      if (monthOrAll().toUpperCase() == "ALL") {
        val dataTable = "fphistorydata"
        for (((cbState, jfState), jbState) <- JBState.jbStateMap) {
          val sql =
            s"""update $dataTable, $peopleTable
               |   set ${dataTable}.jbcbqk='$jbState',
               |       ${dataTable}.jbcbqkDate='${date()}'
               | where ${dataTable}.idcard=${peopleTable}.idcard
               |   and ${peopleTable}.cbzt='$cbState'
               |   and ${peopleTable}.jfzt='$jfState'""".stripMargin
          AuthData2021.execute(sql, true)
        }
      } else {
        val dataTable = "fpmonthdata"
        for (((cbState, jfState), jbState) <- JBState.jbStateMap) {
          val sql =
            s"""update $dataTable, $peopleTable
               |   set ${dataTable}.jbcbqk='$jbState',
               |        ${dataTable}.jbcbqkDate='${date()}'
               | where ${dataTable}.month='${monthOrAll()}'
               |   and ${dataTable}.idcard=${peopleTable}.idcard
               |   and ${peopleTable}.cbzt='$cbState'
               |   and ${peopleTable}.jfzt='$jfState'""".stripMargin
          AuthData2021.execute(sql, true)
        }
      }
    }

    println(s"结束更新居保状态: ${monthOrAll()}特殊缴费类型数据底册")
  }
}

class ExportData extends Subcommand("dcsj") {
  descr("导出表格数据底册")

  val monthOrAll = trailArg[String](descr = "表格数据月份, 例如: 201912, ALL")
  val outputDir = """D:\特殊缴费"""
  val template = """D:\特殊缴费\特殊缴费类型数据底册模板.xlsx"""

  override def execute(): Unit = {
    val fileName = if (monthOrAll().toUpperCase() == "ALL") {
      s"${Formatter.formatDate("yyyy")}年特殊缴费类型数据底册${Formatter.formatDate()}.xlsx"
    } else {
      s"${monthOrAll()}特殊缴费类型数据底册${Formatter.formatDate()}.xlsx"
    }
    Auth.exportData(monthOrAll(), template, s"$outputDir\\$fileName")
  }
}

class ExportChangedData extends Subcommand("sfbg") {
  descr("导出身份变更信息")

  val outputDir = trailArg[String](descr = "导出目录")
  val template = """D:\特殊缴费\批量信息变更模板.xls"""
  val rowsPerExcel = 500

  override def execute(): Unit = {
    if (!Files.exists(outputDir())) {
      Files.createDirectory(outputDir())
    } else {
      println(s"目录已存在: ${outputDir()}")
      return
    }

    println("从 扶贫历史数据底册 和 居保参保人员明细表 导出信息变更表")

    import AuthData2021._

    for ((kind, code) <- JBKind.special.invert) {
      val data: List[(String, String, String)] =
        run(
          historyData
            .join(joinedPersonData)
            .on(_.idCard == _.idCard)
            .filter { case (h, p) =>
              h.jbKind == Some(lift(kind)) &&
                p.jbKind != Some(lift(code)) &&
                p.cbState == Some("1") &&
                p.jfState == Some("1")
            }
            .map { case (_, p) =>
              (p.idCard, p.name.getOrElse(""), lift(code))
            }
        )

      if (data.nonEmpty) {
        println(s"开始导出 $kind 批量信息变更表")

        var i = 0
        var files = 0
        var workbook: Workbook = null
        var sheet: Sheet = null
        val startRow = 1
        var currentRow = startRow

        data.foreach { it =>
          if (i % rowsPerExcel == 0) {
            if (workbook != null) {
              files += 1
              workbook.save(outputDir() / s"${kind}批量信息变更表${files}.xls")
              workbook = null
            }
            if (workbook == null) {
              workbook = Excel.load(template)
              sheet = workbook.getSheetAt(0)
              currentRow = 1
            }
          }
          val row = sheet.getOrCopyRow(currentRow, startRow, false)
          currentRow += 1
          row("B").value = it._1
          row("E").value = it._2
          row("J").value = it._3

          i += 1
        }
        if (workbook != null) {
          files += 1
          workbook.save(outputDir() / s"${kind}批量信息变更表${files}.xls")
        }
        println(s"结束导出 $kind 批量信息变更表: $i 条")
      }
    }
  }
}
