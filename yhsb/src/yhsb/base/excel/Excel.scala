package yhsb.base

package excel

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Path, Paths}

import scala.annotation.tailrec

import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel._
import org.apache.poi.ss.util.CellRangeAddressList
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import yhsb.base.io.AutoClose.use
import yhsb.base.io.Path._
import yhsb.base.math.Number.RichDouble

object Excel {
  val Excel = yhsb.base.excel.Excel

  object ExcelType extends Enumeration {
    type ExcelType = Value
    val Xls, Xlsx, Auto = Value
  }

  import ExcelType._

  def load[T : PathConvertible](file: T, excelType: ExcelType = Auto): Workbook = {
    val path: Path = file
    val fileName = path.toString()
    val ty = excelType match {
      case Auto =>
        val fn = fileName.toLowerCase()
        if (fn.endsWith(".xls")) {
          Xls
        } else if (fn.endsWith(".xlsx")) {
          Xlsx
        } else {
          throw new Exception(s"unknown excel type: $fn")
        }
      case other => other
    }

    def loadFile = {
      new ByteArrayInputStream(
        Files.readAllBytes(path)
      )
    }

    ty match {
      case Xls  => new HSSFWorkbook(loadFile)
      case Xlsx => new XSSFWorkbook(loadFile)
    }
  }

  implicit class WorkbookOps(val book: Workbook) extends AnyVal {
    def save[T : PathConvertible](file: T): Unit = {
      use(Files.newOutputStream(file)) { out =>
        book.write(out)
      }
    }

    def saveIf[T : PathConvertible](condition: => Boolean)(
      file: T, ifTrue: String => Unit, ifFalse: String => Unit
    ) = {
      val path: Path = file
      if (condition) {
        book.save(path)
        ifTrue(path.toString)
      } else {
        ifFalse(path.toString)
      }
    }

    def saveAfter[T : PathConvertible](file: T)(afterAction: String => Unit): Unit = {
      val path: Path = file
      book.save(path)
      afterAction(path.toString())
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

      if (newRow == null) {
        newRow = sheet.createRow(targetRowIndex)
      }
      
      newRow.setHeight(srcRow.getHeight)

      for (index <- srcRow.getFirstCellNum until srcRow.getLastCellNum()) {
        val srcCell = srcRow.getCell(index)
        if (srcCell != null) {
          val newCell = newRow.createCell(index)
          newCell.setCellStyle(srcCell.getCellStyle)
          newCell.setCellComment(srcCell.getCellComment)
          newCell.setHyperlink(srcCell.getHyperlink)

          if (clearValue) {
            newCell.setBlank()
          } else {
            import org.apache.poi.ss.usermodel.CellType._
            srcCell.getCellType match {
              case NUMERIC =>
                newCell.setCellValue(srcCell.getNumericCellValue)
              case STRING =>
                newCell.setCellValue(srcCell.getStringCellValue)
              case FORMULA =>
                newCell.setCellFormula(srcCell.getCellFormula)
              case BLANK => newCell.setBlank()
              case BOOLEAN =>
                newCell.setCellValue(srcCell.getBooleanCellValue)
              case ERROR => newCell.setCellErrorValue(srcCell.getErrorCellValue)
              case _     =>
            }
          }
        }
      }

      val merged = new CellRangeAddressList()
      for (index <- 0 until sheet.getNumMergedRegions) {
        val address = sheet.getMergedRegion(index)
        if (
          sourceRowIndex == address.getFirstRow
          && sourceRowIndex == address.getLastRow
        ) {
          merged.addCellRangeAddress(
            targetRowIndex,
            address.getFirstColumn,
            targetRowIndex,
            address.getLastColumn
          )
        }
      }
      for (region <- merged.getCellRangeAddresses) {
        sheet.addMergedRegion(region)
      }

      newRow
    }

    def getOrCopyRow(
        targetRowIndex: Int,
        sourceRowIndex: Int,
        clearValue: Boolean = true
    ): Row = {
      if (targetRowIndex == sourceRowIndex) {
        val srcRow = sheet.getRow(sourceRowIndex)
        if (srcRow != null && clearValue) srcRow.setBlank()
        srcRow
      } else {
        if (sheet.getLastRowNum >= targetRowIndex) {
          sheet.shiftRows(targetRowIndex, sheet.getLastRowNum, 1, true, false)
        }
        createRow(targetRowIndex, sourceRowIndex, clearValue)
      }
    }

    def copyRows(
        startRowIndex: Int,
        count: Int,
        sourceRowIndex: Int,
        clearValue: Boolean = false
    ) = {
      sheet.shiftRows(startRowIndex, sheet.getLastRowNum, count, true, false)
      for (i <- 0 until count) {
        createRow(startRowIndex + i, sourceRowIndex, clearValue)
      }
    }

    def rowIterator(start: Int, end: Int = -1): Iterator[Row] = {
      new Iterator[Row]() {
        private var index = Math.max(0, start)
        private val last =
          if (end == -1) sheet.getLastRowNum
          else Math.min(end, sheet.getLastRowNum)

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

    def deleteRows(rowIndex: Int, count: Int) = {
      val lastRowNum = sheet.getLastRowNum()
      if (rowIndex >= 0 && rowIndex <= lastRowNum && count > 0) {
        val endRowIndex = Math.min(rowIndex + count - 1, lastRowNum)
        for (index <- rowIndex to endRowIndex) {
          val row = sheet.getRow(index)
          if (row != null) sheet.removeRow(row)
        }
        if (endRowIndex < lastRowNum) {
          sheet.shiftRows(endRowIndex + 1, lastRowNum, rowIndex - endRowIndex - 1)
        }
      }
    }

    def deleteRow(rowIndex: Int) = deleteRows(rowIndex, 1)

    def deleteRowIf(startRow: Int, endRow: Int = sheet.getLastRowNum() + 1)
      (filter: Row => Boolean) = {
      var startIndex = startRow
      var endRowIndex = endRow
      while (startIndex < endRowIndex) {
        var endIndex = startIndex
        var continue = true
        while (endIndex < endRowIndex && continue) {
          if (filter(sheet.getRow(endIndex))) {
            continue = false
          } else {
            endIndex += 1
          }
        }
        def count = endIndex - startIndex
        if (count > 0) {
          sheet.deleteRows(startIndex, count)
          endRowIndex -= count
        } else {
          startIndex += 1
        }
      }
    }
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
        dest.getOrCreateCell(field).setCellValue(value)
      }
    }

    def cellValue[T](
        colName: String,
        getValue: Cell => String = _.value
    )(convert: String => T): Option[T] = {
      val value = getValue(row(colName))
      try {
        if (value != null && value.nonEmpty) Some(convert(value))
        else None
      } catch {
        case _: Exception => None
      }
    }

    def setCellValue[T](colName: String, value: Option[T]) = {
      if (value.isDefined) row(colName).setCellValue(value.get.toString)
    }

    def setBlank() = {
      for (index <- row.getFirstCellNum until row.getLastCellNum()) {
        val cell = row.getCell(index)
        if (cell != null) cell.setBlank()
      }
    }
  }

  implicit class CellOps(val cell: Cell) extends AnyVal {
    def value: String = {
      import org.apache.poi.ss.usermodel.CellType._
      if (cell == null) return ""

      @tailrec
      def getString(typ: CellType): String = {
        typ match {
          case STRING => cell.getStringCellValue
          case NUMERIC =>
            val v = cell.getNumericCellValue
            if (v.isValidInt) v.toInt.toString
            else if (v.isValidLong) v.toLong.toString
            else v.toString
          case BLANK   => ""
          case BOOLEAN => cell.getBooleanCellValue.toString
          case ERROR   => ""
          case FORMULA => getString(cell.getCachedFormulaResultType)
          case ty      => throw new Exception(s"unsupported type: $ty")
        }
      }

      getString(cell.getCellType)
    }

    def value_=(v: String): Unit = {
      if (cell == null) return
      if (v != null) cell.setCellValue(v)
      else cell.setBlank()
    }

    def value_=(v: Double): Unit = {
      if (cell == null) return
      cell.setCellValue(v)
    }

    def value_=(v: BigDecimal): Unit = {
      if (cell == null) return
      if (v != null) cell.setCellValue(v.toDouble)
      else cell.setBlank()
    }

    def value_=(v: Int): Unit = {
      if (cell == null) return
      cell.setCellValue(v.toDouble)
    }

    def value_=[T](v: Option[T]): Unit = {
      if (v.isDefined) {
        v.get match {
          case d: Double => cell.value = d
          case d: BigDecimal => cell.value = d
          case i: Int => cell.value = i
          case s: String => cell.value = s
        }
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
      val name = new StringBuilder()
      while (dividend > 0) {
        val modulo = (dividend - 1) % 26
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
              anchorColumn = value.group(1).nonEmpty,
              colName = value.group(2),
              column = columnNameToNumber(value.group(2)),
              anchorRow = value.group(3).nonEmpty,
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

    override def toString: String = toAddress
  }
}
