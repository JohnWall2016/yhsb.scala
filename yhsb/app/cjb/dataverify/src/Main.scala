package yhsb.app.cjb.dataverify

import yhsb.util.commands._
import yhsb.util.Excel
import yhsb.util.Excel._
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbxxRequest
import yhsb.cjb.net.protocol.Cbxx
import scala.collection.mutable

class Verify(args: Seq[String])
    extends Command(args)
    with InputFile
    with RowRange
    with OutputDir {
  banner("数据核实程序")

  object Type extends Enumeration {
    val Jfry = Value("缴费人员")
    val Dyry = Value("待遇人员")
  }

  case class Data(
    idcard: String,
    name: String,
    oldName: String,
    typ: Type.Value,
  )

  override def execute(): Unit = {
    val workbook = Excel.load(inputFile())
    val sheet = workbook.getSheetAt(0)

    val data = mutable.Map[String, Data]()
    Session.use() { sess =>
      for (r <- (startRow() - 1) until endRow()) {
        val row = sheet.getRow(r)
        val name = row.getCell("C").value.trim
        val idcard = row.getCell("D").value.trim
        sess.sendService(CbxxRequest(idcard))
        val result = sess.getResult[Cbxx]()
        val cbxx = result(0)
        //data(cbxx.dwName) = Data(idcard, name, cbxx.name, if (cbxx))
      }
    }
  }
}

object Main {
  def main(args: Array[String]) = new Verify(args).runCommand()
}
