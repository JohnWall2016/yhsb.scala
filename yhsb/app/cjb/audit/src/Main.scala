package yhsb.app.cjb.audit

import org.rogach.scallop._
import yhsb.util.commands.DateRange
import yhsb.util.commands.Export
import yhsb.util.datetime.formater.toDashedDate
import yhsb.cjb.net.Session
import yhsb.cjb.net.protocol.CbshQuery
import yhsb.cjb.net.protocol.Cbsh

class Conf(args: Seq[String])
    extends ScallopConf(args)
    with DateRange
    with Export {
  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)

    val startDate = conf.startDate.map(toDashedDate(_)).getOrElse("")
    val endDate = conf.endDate.map(toDashedDate(_)).getOrElse("")

    val result = Session.use() { sess =>
      sess.sendService(CbshQuery(startDate, endDate))
      sess.getResult[Cbsh]()
    }

    result.foreach(println(_))
  }
}
