package yhsb.app.cjb.fullcover

import org.rogach.scallop._

class Conf(args: Seq[String]) extends ScallopConf(args) {
  trait RowIndex { _: ScallopConf =>
    val beginRow = trailArg[Int](descr = "开始行")
    val endRow = trailArg[Int](descr = "结束行")
  }

  val updateDwxx = new Subcommand("upDwxx") with RowIndex {
    descr("更新单位信息")

    val row = trailArg[String](descr = "列名称")
  }
  addSubcommand(updateDwxx)
  verify()
}

object Main {
  def main(args: Array[String]) {
    val conf = new Conf(args)
    
    conf.subcommand match {
      case Some(conf.updateDwxx) => {
        import conf.updateDwxx._
        println(s"begin: $beginRow, end: $endRow, row: $row")
      }
      case _ => {
        conf.printHelp()
      }
    }
  }
}
