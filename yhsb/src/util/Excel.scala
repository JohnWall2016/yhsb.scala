package yhsb.util

import org.apache.poi
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Row
import scala.runtime.RichInt
import scala.collection.StringOps
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.nio.file.Files
import java.nio.file.Paths
import scala.annotation.meta.field
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook

import AutoClose.use
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.openxml4j.exceptions.InvalidOperationException
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.nio.file.Path
import scala.util.matching.Regex

object Excel {

  object ExcelType extends Enumeration {
    type ExcelType = Value
    val Xls, Xlsx, Auto = Value
  }

  import ExcelType._

  def load(fileName: String, excelType: ExcelType = Auto): Workbook = {
    val ty = excelType match {
      case Auto => {
        val fn = fileName.toLowerCase()
        if (fn.endsWith(".xls")) {
          Xls
        } else if (fn.endsWith(".xlsx")) {
          Xlsx
        } else {
          throw new Exception(s"unknown excel type: $fn")
        }
      }
      case other => other
    }

    def loadFile = {
      new ByteArrayInputStream(
        Files.readAllBytes(Paths.get(fileName))
      )
    }

    ty match {
      case Xls  => new HSSFWorkbook(loadFile)
      case Xlsx => new XSSFWorkbook(loadFile)
    }
  }

  def load(path: Path): Workbook = load(path.toString(), Auto)

  implicit class WorkbookOps(val book: Workbook) extends AnyVal {
    def save(fileName: String) {
      use(Files.newOutputStream(Paths.get(fileName))) { out =>
        book.write(out)
      }
    }
    def save(file: Path) {
      use(Files.newOutputStream(file)) { out =>
        book.write(out)
      }
    }
  }

  implicit class SheetOps(val sheet: Sheet) extends AnyVal {
    def createRow(
        targetRowIndex: Int,
        sourceRowIndex: Int,
        clearValue: Boolean
    ): Row = {
      if (targetRowIndex == sourceRowIndex) {
        throw new IllegalArgumentException(
          s"sourceIndex and targetIndex cannot be same: $sourceRowIndex == $targetRowIndex"
        )
      }

      var newRow = sheet.getRow(targetRowIndex)
      val srcRow = sheet.getRow(sourceRowIndex)

      /*if (newRow != null) {
        sheet.shiftRows(targetRowIndex, sheet.getLastRowNum(), 1, true, false)
      }*/

      newRow = sheet.createRow(targetRowIndex)
      newRow.setHeight(srcRow.getHeight())

      for (index <- srcRow.getFirstCellNum() until srcRow.getLastCellNum()) {
        val srcCell = srcRow.getCell(index)
        if (srcCell != null) {
          val newCell = newRow.createCell(index)
          newCell.setCellStyle(srcCell.getCellStyle())
          newCell.setCellComment(srcCell.getCellComment())
          newCell.setHyperlink(srcCell.getHyperlink())

          import org.apache.poi.ss.usermodel.CellType._
          srcCell.getCellType() match {
            case NUMERIC =>
              newCell.setCellValue(
                if (clearValue) 0 else srcCell.getNumericCellValue()
              )
            case STRING =>
              newCell.setCellValue(
                if (clearValue) "" else srcCell.getStringCellValue()
              )
            case FORMULA =>
              newCell.setCellFormula(
                if (clearValue) null else srcCell.getCellFormula()
              )
            case BLANK => newCell.setBlank()
            case BOOLEAN =>
              newCell.setCellValue(
                if (clearValue) false else srcCell.getBooleanCellValue()
              )
            case ERROR => newCell.setCellErrorValue(srcCell.getErrorCellValue())
            case _     => {}
          }
        }
      }

      val merged = new CellRangeAddressList()
      for (index <- 0 until sheet.getNumMergedRegions()) {
        val address = sheet.getMergedRegion(index)
        if (
          sourceRowIndex == address.getFirstRow
          && sourceRowIndex == address.getLastRow
        ) {
          merged.addCellRangeAddress(
            targetRowIndex,
            address.getFirstColumn(),
            targetRowIndex,
            address.getLastColumn()
          )
        }
      }
      for (region <- merged.getCellRangeAddresses()) {
        sheet.addMergedRegion(region)
      }

      newRow
    }

    def getOrCopyRow(
        targetRowIndex: Int,
        sourceRowIndex: Int,
        clearValue: Boolean = false
    ): Row = {
      if (targetRowIndex == sourceRowIndex) {
        sheet.getRow(sourceRowIndex)
      } else {
        if (sheet.getLastRowNum() >= targetRowIndex) {
          sheet.shiftRows(targetRowIndex, sheet.getLastRowNum(), 1, true, false)
        }
        createRow(targetRowIndex, sourceRowIndex, clearValue)
      }
    }

    def copyRows(
        startRowIndex: Int,
        count: Int,
        sourceRowIndex: Int,
        clearValue: Boolean = false
    ) {
      sheet.shiftRows(startRowIndex, sheet.getLastRowNum(), count, true, false)
      for (i <- 0 until count) {
        createRow(startRowIndex + i, sourceRowIndex, clearValue)
      }
    }

    def rowIterator(start: Int, end: Int = -1): Iterator[Row] = {
      new Iterator[Row]() {
        private var index = math.max(0, start)
        private val last =
          if (end == -1) sheet.getLastRowNum() - 1
          else math.min(end - 1, sheet.getLastRowNum() - 1)

        def hasNext: Boolean = index <= last

        def next(): Row = {
          val row = sheet.getRow(index)
          index += 1
          row
        }
      }
    }

    def getCell(cellName: String) = {
      val c = CellRef.from(cellName).get
      sheet.getRow(c.rowIndex - 1).getCell(c.columnIndex - 1)
    }

    def getCell(row: Int, col: Int) = 
      sheet.getRow(row).getCell(col)

    def apply(cellName: String) = getCell(cellName)
  }

  implicit class RowOps(val row: Row) extends AnyVal {
    def getCell(columnName: String) =
      row.getCell(CellRef.columnNameToNumber(columnName) - 1)

    def apply(columnName: String) = getCell(columnName)

    def createCell(columnName: String) =
      row.createCell(CellRef.columnNameToNumber(columnName) - 1)

    def getOrCreateCell(col: Int): Cell = {
      var cell = row.getCell(col)
      if (cell == null) cell = row.createCell(col)
      cell
    }

    def getOrCreateCell(columnName: String): Cell =
      getOrCreateCell(CellRef.columnNameToNumber(columnName) - 1)

    def copyTo(dest: Row, fields: String*) = {
      for (field <- fields) {
        val value = getCell(field).value
        getOrCreateCell(field).setCellValue(value)
      }
    }
  }

  implicit class CellOps(val cell: Cell) extends AnyVal {
    def value: String = {
      import org.apache.poi.ss.usermodel.CellType._
      if (cell == null) return ""
      cell.getCellType() match {
        case STRING => cell.getStringCellValue()
        case NUMERIC => {
          val v = cell.getNumericCellValue()
          if (v.isValidInt) v.toInt.toString else v.toString
        }
        case BLANK   => ""
        case BOOLEAN => cell.getBooleanCellValue().toString()
        case ERROR => ""
        case FORMULA => cell.getStringCellValue()
        case ty      => throw new Exception(s"unsupported type: $ty")
      }
    }
  }

  object CellRef {
    def columnNameToNumber(name: String): Int = {
      var sum = 0
      for (ch <- name.toUpperCase()) {
        sum *= 26
        sum += ch - 64
      }
      sum
    }

    def columnNumberToName(number: Int): String = {
      var dividend = number
      var name = new StringBuilder()
      while (dividend > 0) {
        var modulo = (dividend - 1) % 26
        name.append((65 + modulo).toChar)
        dividend = (dividend - modulo) / 26
      }
      name.reverse.toString()
    }

    private val cellRegex = """^(\$?)([A-Z]+)(\$?)(\d+)$""".r

    def from(address: String): Option[CellRef] = {
      cellRegex.findFirstMatchIn(address) match {
        case Some(value) =>
          Some(
            new CellRef(
              anchorColumn = !value.group(1).isEmpty(),
              colName = value.group(2),
              column = columnNameToNumber(value.group(2)),
              anchorRow = !value.group(3).isEmpty(),
              row = value.group(4).toInt
            )
          )
        case None => None
      }
    }
  }

  class CellRef(
      row: Int,
      column: Int,
      anchor: Boolean = false,
      anchorRow: Boolean = false,
      anchorColumn: Boolean = false,
      colName: String = null
  ) {
    val rowIndex = row
    val columnIndex = column
    val anchored = anchor
    val rowAnchored = anchor || anchorRow
    val columnAnchored: Boolean = anchor || anchorColumn
    val columnName: String =
      if (colName != null) colName else CellRef.columnNumberToName(column)

    def toAddress: String = {
      val sb = new StringBuilder()
      if (columnAnchored) sb.append("$")
      sb.append(columnName)
      if (rowAnchored) sb.append("$")
      sb.append(s"$row")
      sb.toString()
    }

    override def toString(): String = toAddress
  }
}
