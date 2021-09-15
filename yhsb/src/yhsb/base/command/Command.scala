package yhsb.base.command

import org.rogach.scallop.{Subcommand => ScallopSubcommand, _}
import org.rogach.scallop.exceptions.ScallopResult
import org.rogach.scallop.exceptions.Help
import org.rogach.scallop.exceptions.Version
import org.rogach.scallop.exceptions.ScallopException

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

trait OutputFile { _: ScallopConf =>
  val outputFile = trailArg[String](descr = "输出文件路径")
}

trait SheetIndex { _: ScallopConf =>
  val sheetIndex =
    trailArg[Int](descr = "数据表序号, 默认为0", default = Some(0), required = false)
}

trait SheetName { _: ScallopConf =>
  val sheetName = trailArg[String](descr = "数据表名称")
}

trait SheetIndexOpt { _: ScallopConf =>
  val sheetIndex =
    opt[Int](name = "sheetIndex", descr = "数据表序号, 默认为0", default = Some(0))
}

trait InputDir { _: ScallopConf =>
  val inputDir = trailArg[String](descr = "文件导入路径")
}

trait OutputDir { _: ScallopConf =>
  val outputDir = trailArg[String](descr = "文件导出路径")
}

trait OutputDirOpt { _: ScallopConf =>
  val outputDir = opt[String](name = "out", short = 'o', descr = "文件导出路径")
}

abstract class Subcommand(commandNameAndAliases: String*)
    extends ScallopSubcommand(commandNameAndAliases: _*) {
  def execute(): Unit
}

class Command(args: collection.Seq[String]) extends ScallopConf(args) {
  shortSubcommandsHelp()

  def addSubCommand(cmd: Subcommand) = addSubcommand(cmd)

  def runCommand() = {
    subcommand match {
      case Some(exec: Subcommand) => exec.execute()
      case None                   => execute()
      case _                      => printHelp()
    }
  }

  def execute(): Unit = printHelp()

  override def assertVerified(): Unit = {
    if (!verified) verify()
    super.assertVerified()
  }

  override protected def onError(e: Throwable): Unit = e match {
    case r: ScallopResult if !throwError.value => r match {
      case Help("") =>
        builder.printHelp()
        Command.exit(0)
        //Compat.exit(0)
      case Help(subname) =>
        builder.findSubbuilder(subname).get.printHelp()
        Command.exit(0)
        //Compat.exit(0)
      case Version =>
        builder.vers.foreach(println)
        Command.exit(0)
        //Compat.exit(0)
      case ScallopException(message) => errorMessageHandler(message)
    }
    case e => throw e
  }

  errorMessageHandler = { message =>
    if (overrideColorOutput.value.getOrElse(System.console() != null)) {
      Console.err.println("[\u001b[31m%s\u001b[0m] Error: %s".format(printedName, message))
    } else {
      // no colors on output
      Console.err.println("[%s] Error: %s".format(printedName, message))
    }
    Command.exit(1)
    //Compat.exit(1)
  }
}

case class ExitException(val code: Int) extends Exception

object Command {
  object ExitMode extends Enumeration {
    val systemExit = Value
    val throwException = Value
  }

  private var exitMode: ExitMode.Value = ExitMode.systemExit

  def setExitMode(mode: ExitMode.Value) = {
    exitMode = mode
  }

  def getExitMode: ExitMode.Value = exitMode

  def exit(code: Int) = {
    if (exitMode == ExitMode.systemExit) {
      Compat.exit(code)
    } else if (exitMode == ExitMode.throwException) {
      throw ExitException(code)
    }
  }
}