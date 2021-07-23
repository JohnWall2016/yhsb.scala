package yhsb.base.db

import io.getquill.ast.Entity
import io.getquill.context.jdbc.JdbcContext
import yhsb.base.excel.Excel._

import java.nio.file.Files
import scala.collection.mutable

import org.apache.poi.ss.usermodel.Sheet

object Context {
  implicit class JdbcContextOps(val context: JdbcContext[_, _]) {
    def loadExcel[T: Loadable](
        entity: Entity,
        excel: T,
        startRow: Int,
        endRow: Int = -1,
        fields: Seq[String] = Seq(),
        noQuoted: Seq[String] = null,
        printSql: Boolean = false,
        ident: String = "",
        tableIndex: Int = 0,
        polish: ( /*field:*/ String, /*value:*/ String) => String = { (_, v) =>
          v
        }
    ): Long = {
      val workbook = implicitly[Loadable[T]].apply(excel)
      val sheet = workbook.getSheetAt(tableIndex)
      val regex = "(?i)^([A-Z]+)$".r

      val builder = new StringBuilder()

      val endRow_ = if (endRow == -1) sheet.getLastRowNum() + 1 else endRow

      for (index <- (startRow - 1) until endRow_) {
        try {
          val values = mutable.ArrayBuffer[String]()
          for (row <- fields) {
            val value = row match {
              case regex(r) =>
                val value = polish(r, sheet.getRow(index).getCell(r).value)
                if (noQuoted == null || !noQuoted.contains(r)) {
                  s"'$value'"
                } else {
                  value
                }
              case s"`$s`" => s
              case _       => row
            }
            values.addOne(value)
          }
          builder.append(values.mkString(","))
          builder.addAll("\n")
        } catch {
          case ex: Exception =>
            throw new Exception(
              s"loadExcel error at row ${index + 1}",
              ex
            )
        }
      }

      val cvsFile = Files.createTempFile("yhsb", ".cvs")
      Files.writeString(cvsFile, builder)

      val cvsFilePath = cvsFile.toString.replace('\\', '/')
      val tableName = entity.name

      val sql = s"load data infile '$cvsFilePath' into table `$tableName` " +
        "CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY " +
        "ENCLOSED BY '\\'' LINES TERMINATED BY '\\n';"

      execute(
        sql,
        printSql,
        ident,
        afterExecute = Files.deleteIfExists(cvsFile)
      )
    }

    def loadSheet(
        entity: Entity,
        sheet: Sheet,
        startRow: Int,
        endRow: Int = -1,
        fields: Seq[String] = Seq(),
        noQuoted: Seq[String] = null,
        printSql: Boolean = false,
        ident: String = "",
        tableIndex: Int = 0,
        polish: ( /*field:*/ String, /*value:*/ String) => String = { (_, v) =>
          v
        }
    ): Long = {
      val regex = "(?i)^([A-Z]+)$".r

      val builder = new StringBuilder()

      val endRow_ = if (endRow == -1) sheet.getLastRowNum() + 1 else endRow

      for (index <- (startRow - 1) until endRow_) {
        try {
          val values = mutable.ArrayBuffer[String]()
          for (row <- fields) {
            val value = row match {
              case regex(r) =>
                val value = polish(r, sheet.getRow(index).getCell(r).value)
                if (noQuoted == null || !noQuoted.contains(r)) {
                  s"'$value'"
                } else {
                  value
                }
              case s"`$s`" => s
              case _       => row
            }
            values.addOne(value)
          }
          builder.append(values.mkString(","))
          builder.addAll("\n")
        } catch {
          case ex: Exception =>
            throw new Exception(
              s"loadExcel error at row ${index + 1}",
              ex
            )
        }
      }

      val cvsFile = Files.createTempFile("yhsb", ".cvs")
      Files.writeString(cvsFile, builder)

      val cvsFilePath = cvsFile.toString.replace('\\', '/')
      val tableName = entity.name

      val sql = s"load data infile '$cvsFilePath' into table `$tableName` " +
        "CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY " +
        "ENCLOSED BY '\\'' LINES TERMINATED BY '\\n';"

      execute(
        sql,
        printSql,
        ident,
        afterExecute = Files.deleteIfExists(cvsFile)
      )
    }

    def execute(
        sql: String,
        printSql: Boolean = false,
        ident: String = "",
        afterExecute: => Unit = {}
    ): Long = {
      if (printSql) println(s"$ident$sql")
      val result = context.executeAction(sql)
      afterExecute
      result
    }
  }
}
