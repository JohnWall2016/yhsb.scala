package yhsb.db

import io.getquill.context.jdbc.JdbcContext
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.NamingStrategy
import io.getquill.ast.Entity

import yhsb.util.Excel
import yhsb.util.Excel.{RowOps, CellOps}
import scala.util.matching.Regex
import scala.collection.mutable
import java.nio.file.Paths
import java.nio.file.Files
import java.net.URI

object Context {
  implicit class JdbcContextOps(val context: JdbcContext[_, _]) {
    def loadExcel(
        entity: Entity,
        fileName: String,
        startRow: Int,
        endRow: Int,
        fields: Seq[String],
        noQuoted: Seq[String] = null,
        printSql: Boolean = false,
        ident: String = "",
        tableIndex: Int = 0
    ): Long = {
      val workbook = Excel.load(fileName)
      val sheet = workbook.getSheetAt(tableIndex)
      val regex = "(?i)^[A-Z]+$".r

      val builder = new StringBuilder()

      for (index <- (startRow - 1) until endRow) {
        try {
          val values = mutable.ArrayBuffer[String]()
          for (row <- fields) {
            var value = row
            if (regex.matches(row)) {
              value = sheet.getRow(index).getCell(row).value
              if (noQuoted == null || !noQuoted.contains(row)) {
                value = s"'$value'"
              }
            }
            values.addOne(value)
          }
          builder.append(values.mkString(","))
          builder.addAll("\n")
        } catch {
          case ex: Exception => {
            throw new Exception(
              s"loadExcel error at row ${index + 1}",
              ex
            )
          }
        }
      }

      val cvsFile = Files.createTempFile("yhsb", ".cvs")
      Files.writeString(cvsFile, builder)

      val cvsFilePath = new URI(cvsFile.toString()).getPath()
      val tableName = entity.name

      var sql = s"load data infile '$cvsFilePath' into table `$tableName` " +
        "CHARACTER SET utf8 FIELDS TERMINATED BY ',' OPTIONALLY " +
        "ENCLOSED BY '\'' LINES TERMINATED BY '\n';"

      if (printSql) println(s"$ident$sql")

      val result = context.executeAction(sql)

      Files.deleteIfExists(cvsFile)

      result
    }
  }
}