package yhsb

import utest.{TestSuite, Tests, test}
import base.excel.Excel

object ExcelTest extends TestSuite {
  def tests =
    Tests {
      test("loadExcel") {
        val book =  Excel.load("""D:\参保管理\参保全覆盖2\下发数据清册\雨湖区全覆盖下发数据清册1.xlsx""")
        val sheet = book.getSheetAt(0)
        println(sheet.getRow(2).getCell(1).getStringCellValue)
        println(sheet.getRow(2).getCell(2).getStringCellValue)
        println(sheet.getRow(2).getCell(2).setCellValue("abc"))
        book.close()
      }
      test("insertRow") {
        println("TODO: 未实现")
      }
    }
}
