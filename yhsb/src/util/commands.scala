package yhsb.util.commands

import org.rogach.scallop._

trait RowRange { _: ScallopConf =>
  val startRow = trailArg[Int](descr = "开始行, 从1开始")
  val endRow = trailArg[Int](descr = "结束行, 包含在内")
}

trait DateRange { _: ScallopConf =>
  val startDate = trailArg[String](descr = "开始时间, 例如: 20200701")
  val endDate = trailArg[String](descr = "结束时间, 例如: 20200723", required = false)
}

trait Export { _: ScallopConf =>
  val export = opt[Boolean](required = false, descr = "是否导出数据")
}

trait InputFile { _: ScallopConf =>
  val inputFile = trailArg[String](descr = "输入文件路径")
}

trait SheetIndex { _: ScallopConf =>
  val sheetIndex =
    trailArg[Int](descr = "数据表序号, 默认为0", default = Some(0), required = false)
}

trait OutputDir { _: ScallopConf =>
  val outputDir = trailArg[String](descr = "文件导出路径")
}

trait OutputDirOpt { _: ScallopConf =>
  val outputDir = opt[String](name = "out", short = 'o', descr = "文件导出路径")
}

trait Executable {
  def execute(): Unit
}

abstract class SubExecutable(commandNameAndAliases: String*)
    extends Subcommand(commandNameAndAliases: _*)
    with Executable

class ExecutiveConf(args: Seq[String]) {
  private val conf = new ScallopConf(args) {
    shortSubcommandsHelp()
  }

  def banner(b: String) = conf.banner(b)

  def addCommands(cmds: (Subcommand with Executable)*) =
    cmds.foreach(conf.addSubcommand(_))

  def addCommand(cmd: Subcommand with Executable) =
    conf.addSubcommand(cmd)

  def execute() = {
    conf.subcommand match {
      case Some(exec: Executable) => exec.execute()
      case _                      =>
    }
  }

  def verify() = conf.verify()
}
