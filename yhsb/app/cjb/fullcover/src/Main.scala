package yhsb.app.cjb.fullcover

import org.rogach.scallop._

import java.nio.file.Paths

import yhsb.util.commands.RowRange
import yhsb.util.commands.InputFile
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.db.FullCover._
import yhsb.util.commands.{Subcommand => _, _}
import yhsb.util.Strings.StringOps
import yhsb.db.Context.JdbcContextOps
import yhsb.cjb.net.protocol.JBKind
import yhsb.util.Files.appendToFileName

import yhsb.util.BiMap

class Conf(args: Seq[String]) extends ScallopConf(args) {
  shortSubcommandsHelp()

  banner("全覆盖数据处理程序")

  val updateDwmc = new Subcommand("upDwmc") with InputFile with RowRange {
    descr("更新单位名称")

    val idcardRow = trailArg[String](descr = "身份证列名称")
    val dwxxRow = trailArg[String](descr = "单位信息列名称")
  }

  val importJB = new Subcommand("importJB") with InputFile with RowRange {
    descr("导入居保参保人员明细表")

    val clear =
      opt[Boolean](descr = "是否清除数据表", required = false, default = Some(false))
  }

  val importFC =
    new Subcommand("importFC") with InputFile with RowRange with SheetIndex {
      descr("导入全覆盖2省厅下发原始数据")

      val clear =
        opt[Boolean](descr = "是否清除数据表", required = false, default = Some(false))
    }

  val updateZhqk = new Subcommand("upZhqk") {
    descr("根据已记录数据更新综合情况")
  }

  val downloadByDwmc = new Subcommand("downByDw") {
    descr("按单位导出下发数据")

    val outputDir = opt[String](
      name = "out",
      short = 'o',
      descr = "文件导出路径",
      default = Option("""D:\参保管理\参保全覆盖2\导出目录""")
    )

    val dwmc = trailArg[List[String]](
      descr = "导出单位名称, 默认显示可导出单位; ALL - 导出所有单位",
      required = false
    )

    val tmplXlsx = """D:\参保管理\参保全覆盖2\雨湖区全覆盖下发数据清册模板2.xlsx"""
  }

  val auditData = new Subcommand("auditData") with InputFile with RowRange {
    descr("审核数据")

    val outputDir = opt[String](
      name = "out",
      short = 'o',
      descr = "文件导出路径",
      default = Option("""D:\参保管理\参保全覆盖2\乡镇街上报数据\数据审核""")
    )

    val tmplXlsx = """D:\参保管理\参保全覆盖2\乡镇街上报数据\全覆盖下发数据清册审核模板.xlsx"""
  }

  val convertData = new Subcommand("convertData") with InputFile with RowRange {
    descr("转换数据")

    val outputDir = opt[String](
      name = "out",
      short = 'o',
      descr = "文件导出路径",
      default = Option("""D:\参保管理\参保全覆盖2\乡镇街上报数据\数据转换""")
    )

    val tmplXlsx = """D:\参保管理\参保全覆盖2\乡镇街上报数据\qfgplcjmode.xls"""

    val operator = trailArg[String](descr = "采集人")
    val phone = trailArg[String](descr = "采集人电话")
  }

  addSubcommand(updateDwmc)
  addSubcommand(importJB)
  addSubcommand(importFC)
  addSubcommand(updateZhqk)
  addSubcommand(downloadByDwmc)

  addSubcommand(auditData)
  addSubcommand(convertData)

  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)

    conf.subcommand match {
      case Some(conf.updateDwmc)     => updateDwmc(conf)
      case Some(conf.importJB)       => importJB(conf)
      case Some(conf.importFC)       => importFC(conf)
      case Some(conf.updateZhqk)     => updateZhqk(conf)
      case Some(conf.downloadByDwmc) => downloadByDwmc(conf)
      case Some(conf.auditData)      => auditData(conf)
      case Some(conf.convertData)    => convertData(conf)
      case _                         => conf.printHelp()
    }
  }

  def updateDwmc(conf: Conf) = {
    import conf.updateDwmc._
    println(
      s"begin: ${startRow()}, end: ${endRow()}, " +
        s"idcardRow: ${idcardRow()}, dwxxRow: ${dwxxRow()}"
    )

    val workbook = Excel.load(inputFile())
    val sheet = workbook.getSheetAt(0)

    import fullcover._

    val update = (idcard: String, dwxx: String) =>
      run(
        fc2Stxfsj
          .filter(_.idcard == lift(idcard))
          .update(_.dwmc -> lift(Option(dwxx)))
      )

    for (index <- (startRow() - 1) until endRow()) {
      val row = sheet.getRow(index)
      val idcard = row.getCell(idcardRow()).value
      val dwxx = row.getCell(dwxxRow()).value
      println(s"${index + 1} $idcard $dwxx")
      update(idcard, dwxx)
    }
  }

  def importJB(conf: Conf) = {
    import fullcover._
    import conf.importJB._

    if (clear()) {
      println("清除数据表: 居保参保人员明细表")
      run(jbrymx.delete)
    }

    println("开始导入居保参保人员明细表")
    fullcover.loadExcel(
      jbrymx.quoted,
      inputFile(),
      startRow(),
      endRow(),
      Seq("E", "A", "B", "C", "F", "G", "I", "K", "L", "O"),
      printSql = true
    )
    println("结束导入居保参保人员明细表")
  }

  def importFC(conf: Conf) = {
    import fullcover._
    import conf.importFC._

    if (clear()) {
      println("清除数据表: 全覆盖2省厅下发原始数据")
      run(fc2Stxfyssj.delete)
    }

    println("开始导入全覆盖2省厅下发原始数据")
    fullcover.loadExcel(
      fc2Stxfyssj.quoted,
      inputFile(),
      startRow(),
      endRow(),
      Seq("D", "C", "E", "F", "G"),
      tableIndex = sheetIndex(),
      printSql = true
    )
    println("结束导入全覆盖2省厅下发原始数据")
  }

  def updateZhqk(conf: Conf) = {
    import fullcover._

    // 是否在校生
    println("更新 高校学生数据")
    run(
      fc2Stxfsj
        .filter(_.inZxxssj == Some("1"))
        .update(_.zhqk -> Some("在校学生"), _.memo -> Some("高校学生数据"))
    )

    // 户籍状态
    println("更新 户籍状态数据")
    run(
      fc2Stxfsj
        .filter(e => e.manageName == Some("迁出") || e.manageName == Some("注销"))
        .update(
          _.zhqk -> Some("其他人员"),
          e => e.memo -> Some("户籍状态: " + e.manageName)
        )
    )
    run(
      fc2Stxfsj
        .filter(_.manageName == Some("死亡"))
        .update(_.zhqk -> Some("死亡未销户人员"), e => e.memo -> Some("户籍状态: 死亡"))
    )

    // 之前全覆盖落实情况
    println("更新 之前全覆盖落实情况数据")
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("自愿放弃"))
        .update(_.zhqk -> Some("无参保意愿"), _.memo -> Some("之前落实情况: 自愿放弃"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("已录入居保"))
        .update(_.zhqk -> Some("我区参加居保"), _.memo -> Some("之前落实情况: 已录入居保"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("参职保（含退休）"))
        .update(_.zhqk -> Some("参加省职保"), _.memo -> Some("之前落实情况: 参职保（含退休）"))
    )
    run(
      fc2Stxfsj
        .filter(e =>
          e.hsqk == Some("数据错误") || e.hsqk == Some("户口不在本地") || e.hsqk == Some(
            "空挂户"
          )
        )
        .update(
          _.zhqk -> Some("其他人员"),
          e => e.memo -> Some("之前落实情况: " + e.hsqk)
        )
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("死亡（失踪）"))
        .update(_.zhqk -> Some("死亡未销户人员"), _.memo -> Some("之前落实情况: 死亡（失踪）"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("服刑人员"))
        .update(_.zhqk -> Some("服刑人员"), _.memo -> Some("之前落实情况: 服刑人员"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("参军"))
        .update(_.zhqk -> Some("现役军人"), _.memo -> Some("之前落实情况: 参军"))
    )
    run(
      fc2Stxfsj
        .filter(_.hsqk == Some("16岁以上在校生"))
        .update(_.zhqk -> Some("在校学生"), _.memo -> Some("之前落实情况: 16岁以上在校生"))
    )

    // 数据比对结果
    println("更新 数据比对结果")
    run(
      fc2Stxfsj
        .filter(_.swcb == Some("机关事业"))
        .update(_.zhqk -> Some("参加机关保"), _.memo -> Some("比对结果: 省外机关养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.swcb == Some("企业职工"))
        .update(_.zhqk -> Some("参加省外职保"), _.memo -> Some("比对结果: 省外职工养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.swcb == Some("城乡居民"))
        .update(_.zhqk -> Some("其他人员"), _.memo -> Some("比对结果: 省外居民养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.slcb == Some("机关事业"))
        .update(_.zhqk -> Some("参加机关保"), _.memo -> Some("比对结果: 省内机关养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.slcb == Some("企业职工"))
        .update(_.zhqk -> Some("参加省职保"), _.memo -> Some("比对结果: 省内职工养老保险"))
    )
    run(
      fc2Stxfsj
        .filter(_.slcb == Some("城乡居民"))
        .update(_.zhqk -> Some("其他人员"), _.memo -> Some("比对结果: 省内居民养老保险"))
    )

    // 我区参加居保
    println("更新 我区参加居保结果")
    run(
      fc2Stxfsj
        .filter(_.inSfwqjb == Some("1"))
        .update(
          _.zhqk -> Some("我区参加居保"),
          e => e.memo -> Some("参保身份: " + e.wqjbsf)
        )
    )

  }

  def downloadByDwmc(conf: Conf) = {
    import conf.downloadByDwmc._

    import fullcover._

    if (dwmc.isEmpty) {
      val result = run(
        fc2Stxfsj
          .filter(e => e.xfpc == Some("第二批"))
          .groupBy(_.dwmc)
          .map(e => (e._1, e._2.size))
      )

      var sum: Long = 0
      var yhsTotal: Long = 0
      for ((name, count) <- result) {
        val yhs = run(
          fc2Stxfsj
            .filter(e =>
              e.xfpc == Some("第二批") && e.dwmc == lift(name) && e.zhqk.isDefined
            )
            .size
        )
        println(
          f"${name.getOrElse("").padRight(10)} $count%6d $yhs%6d ${count - yhs}%6d"
        )
        sum += count
        yhsTotal += yhs
      }
      println(f"${"合计".padRight(10)} $sum%6d $yhsTotal%6d ${sum - yhsTotal}%6d")
    } else {
      var dwmcs = dwmc()
      if (dwmcs(0).toUpperCase() == "ALL") {
        val result = run(
          fc2Stxfsj.groupBy(_.dwmc).map(_._1)
        )
        dwmcs = result.map {
          case None        => null
          case Some(value) => value
        }
      }
      for (dw <- dwmcs) {
        val file = Paths.get(outputDir(), s"${dw}全覆盖数据底册.xlsx")
        println(s"导出 $dw => $file")
        val result: List[FC2Stxfsj] = run(
          fc2Stxfsj
            .filter(e => e.xfpc == Some("第二批") && e.dwmc == lift(Option(dw)))
            .sortBy(e => (e.address, e.name))
          //.take(10)
        )
        if (!result.isEmpty) {
          val workbook = Excel.load(tmplXlsx)
          val sheet = workbook.getSheetAt(0)
          var index, start = 2
          for (e <- result) {
            println(s"${index - start + 1} ${e.name} ${e.idcard}")
            val row = sheet.getOrCopyRow(index, start)
            row.getCell("A").setCellValue(index - start + 1)
            row.getCell("B").setCellValue(e.name)
            row.getCell("C").setCellValue(e.idcard)
            row.getCell("D").setCellValue(e.address.getOrElse(""))
            row.getCell("E").setCellValue(e.manageName.getOrElse(""))
            row.getCell("F").setCellValue(e.hsqk.getOrElse(""))
            row.getCell("G").setCellValue(e.slcb.getOrElse(""))
            row.getCell("H").setCellValue(e.swcb.getOrElse(""))
            row
              .getCell("I")
              .setCellValue(
                e.inZxxssj
                  .map { case "1" => "是"; case _ => "" }
                  .getOrElse("")
              )
            row
              .getCell("J")
              .setCellValue(
                e.inZxxssj
                  .map { case "1" => "是"; case _ => "" }
                  .getOrElse("")
              )
            row.getCell("K").setCellValue(e.dwmc.getOrElse(""))

            row.getCell("O").setCellValue(e.zhqk.getOrElse(""))
            row.getCell("P").setCellValue(e.memo.getOrElse(""))

            index += 1
          }
          workbook.save(file)
        }
      }
    }
  }

  val hsqkMap = new BiMap(
    "参加省职保" -> ("0", "200"),
    "参加机关保" -> ("0", "201"),
    "参加省外职保" -> ("0", "202"),
    "在校学生" -> ("0", "203"),
    "现役军人" -> ("0", "204"),
    "服刑人员" -> ("0", "205"),
    "死亡未销户人员" -> ("0", "207"),
    "出国（境）人员" -> ("0", "206"),
    "其他人员" -> ("0", "208"),
    "我区参加居保" -> ("1", "011"),
    "无参保意愿" -> ("2", "011")
  )

  val dwmcMap = new BiMap(
    "长城乡" -> "43030201",
    "昭潭街道" -> "43030202",
    "先锋街道" -> "43030203",
    "万楼街道" -> "43030204",
    "楠竹山镇" -> "43030206",
    "姜畲镇" -> "43030207",
    "鹤岭镇" -> "43030208",
    "城正街街道" -> "43030209",
    "雨湖路街道" -> "43030210",
    "云塘街道" -> "43030212",
    "窑湾街道" -> "43030213",
    "广场街道" -> "43030215"
  )

  def auditData(conf: Conf) = {
    import conf.auditData._
    import scala.collection.mutable
    import fullcover._

    val workbook = Excel.load(inputFile())
    val sheet = workbook.getSheetAt(0)

    val outWorkbook = Excel.load(tmplXlsx)
    val thisSheet = outWorkbook.getSheetAt(0)
    val otherSheet = outWorkbook.getSheetAt(1)

    var thisIndex, otherIndex = 2
    val copyIndex = 2

    for (i <- (startRow() - 1) until endRow()) {
      val row = sheet.getRow(i)
      val no = row.getCell("A").value
      val name = row.getCell("B").value
      val idcard = row.getCell("C").value
      val address = row.getCell("D").value
      val hjzt = row.getCell("E").value
      val zqqk = row.getCell("F").value
      val slcb = row.getCell("G").value
      val swcb = row.getCell("H").value
      val sfzxs = row.getCell("I").value
      val sfwqcb = row.getCell("J").value
      val dwmc = row.getCell("K").value
      val cs = row.getCell("L").value
      val cz = row.getCell("M").value
      val code = row.getCell("N").value
      val hsqk = row.getCell("O").value
      val memo = row.getCell("P").value

      val outRow =
        if (cs.isEmpty && cz.isEmpty && code.isEmpty) {
          otherSheet.getOrCopyRow(otherIndex, copyIndex)
        } else {
          thisSheet.getOrCopyRow(thisIndex, copyIndex)
        }

      outRow.getCell("A").setCellValue(no)
      outRow.getCell("B").setCellValue(name)
      outRow.getCell("C").setCellValue(idcard)
      outRow.getCell("D").setCellValue(address)
      outRow.getCell("E").setCellValue(hjzt)
      outRow.getCell("F").setCellValue(zqqk)
      outRow.getCell("G").setCellValue(slcb)
      outRow.getCell("H").setCellValue(swcb)
      outRow.getCell("I").setCellValue(sfzxs)
      outRow.getCell("J").setCellValue(sfwqcb)
      outRow.getCell("K").setCellValue(dwmc)
      outRow.getCell("L").setCellValue(cs)
      outRow.getCell("M").setCellValue(cz)
      outRow.getCell("N").setCellValue(code)
      outRow.getCell("O").setCellValue(hsqk)
      outRow.getCell("P").setCellValue(memo)

      if (cs.isEmpty && cz.isEmpty && code.isEmpty) {
        otherIndex += 1

        val error = "12位行政区划编码 为空"
        println(s"第 ${i + 1} 行 $error")
      } else {
        thisIndex += 1

        val errors = mutable.ListBuffer[String]()

        val data: List[FC2Stxfsj] = run(
          fc2Stxfsj.filter(e =>
            e.idcard == lift(idcard) && e.xfpc == Some("第二批")
          )
        )

        if (data.isEmpty) {
          errors.append("身份证号码")
        } else {
          val dname = data(0).name
          if (dname != name)
            errors.append(s"姓名($dname)")
        }

        dwmcMap.get(dwmc) match {
          case Some(value) => {
            if (code.length != 12 || !code.startsWith(value))
              errors.append("12位行政区划编码")
          }
          case None => errors.append("单位名称")
        }

        hsqkMap.get(hsqk) match {
          case None        => errors.append("核实情况类型")
          case Some(value) =>
        }

        val error = if (!errors.isEmpty) Some(errors.mkString(",") + " 有误") else None

        errors.clear()

        if (error.isDefined) errors.append(error.get)

        if (hsqk == "在校学生") {
          import java.time._
          import java.time.temporal._
          val old = YearMonth.parse(
            idcard.substring(6, 12), 
            format.DateTimeFormatter.ofPattern("yyyyMM")
          ).until(YearMonth.now, ChronoUnit.YEARS)
          if (old >= 25) errors.append("25岁以上在校学生")
        }

        val err = if (!errors.isEmpty) errors.mkString(";") else ""

        println(s"第 ${i + 1} 行 $err")

        outRow.getCell("Q").setCellValue(err)
      }
    }

    outWorkbook.save(
      Paths.get(
        outputDir(),
        appendToFileName(Paths.get(inputFile()).getFileName(), "审核结果")
      )
    )
  }

  def convertData(conf: Conf) = {
    import conf.convertData._
    import scala.collection.mutable

    val workbook = Excel.load(inputFile())
    val sheet = workbook.getSheetAt(0)

    val outWorkbook = Excel.load(tmplXlsx)
    val outSheet = outWorkbook.getSheetAt(0)

    var index, copyIndex = 1

    for (i <- (startRow() - 1) until endRow()) {
      val row = sheet.getRow(i)
      val name = row.getCell("B").value
      val idcard = row.getCell("C").value
      val address = row.getCell("D").value
      val dwmc = row.getCell("K").value
      val csq = row.getCell("L").value
      val code = row.getCell("N").value
      val hsqk = row.getCell("O").value
      val memo = row.getCell("P").value

      val info = hsqkMap.get(hsqk)

      if (info.isEmpty) {
        println(s"第 ${index + 1} 行 核实情况类型 有误")
      } else {
        val outRow = if (index < 89) {
          outSheet.getRow(index)
        } else {
          outSheet.getOrCopyRow(index, copyIndex, false)
        }
        index += 1

        outRow.getCell("A").setCellValue(name)
        outRow.getCell("B").setCellValue("01")
        outRow.getCell("C").setCellValue(idcard)
        outRow
          .getCell("D")
          .setCellFormula(
            s"""IF(B$index="01",IF(LEN(C$index)=0,"",IF(LEN(C$index)<>18,"请输入18位身份证号",IF(MOD(MID(C$index,17,1),2)=1,"1","2"))),"")"""
          )
        outRow
          .getCell("D")
          .setCellValue(
            if ((idcard.charAt(16) - '0') % 2 == 0) "2"
            else "1"
          )

        outRow.getCell("E").setCellValue("01")

        outRow
          .getCell("F")
          .setCellFormula(
            s"""IF(B$index="01",IF(LEN(C$index)=0,"",IF(LEN(C$index)<>18,"请输入18位身份证号",MID(C$index,7,8))),"")"""
          )
        outRow.getCell("F").setCellValue(idcard.substring(6, 14))

        outRow.getCell("G").setCellValue(info.get._1)
        outRow.getCell("H").setCellValue(info.get._2)

        outRow
          .getCell("I")
          .setCellValue(
            if (dwmc.endsWith("街道") || csq.endsWith("社区") || csq.endsWith("机关"))
              "10"
            else "20"
          )

        outRow.getCell("J").setCellValue(code)
        outRow.getCell("K").setCellValue(operator())
        outRow.getCell("L").setCellValue(phone())
        outRow.getCell("M").setCellValue("0")
        outRow.getCell("N").setCellValue(address)
        outRow.getCell("Q").setCellValue(memo)
      }
    }

    outWorkbook.save(
      Paths.get(
        outputDir(),
        appendToFileName(Paths.get(inputFile()).getFileName(), "(批量导入)")
      )
    )
  }
}
