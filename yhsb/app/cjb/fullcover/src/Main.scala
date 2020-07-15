package yhsb.app.cjb.fullcover

import org.rogach.scallop._

class Conf(args: Seq[String]) extends ScallopConf(args) {
  val updateDwxx = new Subcommand("upDwxx") {
    descr("更新单位信息")

    val beginRow = trailArg[Int](descr = "开始行")
    val endRow = trailArg[Int](descr = "结束行")
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
        println(s"begin: $beginRow, end: $endRow")
      }
      case _ => {
        conf.printHelp()
      }
    }
  }
}
