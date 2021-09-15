package yhsb.app.cjb

import scala.collection.mutable.LinkedHashMap

import yhsb.base.collection.BiMap
import yhsb.base.command.Command
import yhsb.base.command.ExitException
import yhsb.base.io.Repl
import yhsb.base.reflect.UnsafeAllocator
import yhsb.base.text.String._
import org.jline.reader.impl.completer.ArgumentCompleter
import org.jline.reader.impl.completer.StringsCompleter
import org.jline.reader.impl.completer.NullCompleter
import org.jline.reader.impl.completer.AggregateCompleter
import yhsb.base.command.Subcommand

object Main {
  implicit class CommandClass(cmdClass: Class[_ <: Command]) {
    def banner = {
      cmdClass
        .getConstructor(classOf[collection.Seq[String]])
        .newInstance(Seq())
        .builder
        .bann
        .getOrElse("")
    }

    def runCommand(args: collection.Seq[String]) = {
      cmdClass
        .getConstructor(classOf[collection.Seq[String]])
        .newInstance(args)
        .runCommand()
    }

    def subCommandNames = {
      val builder = cmdClass
        .getConstructor(classOf[collection.Seq[String]])
        .newInstance(Seq())
        .builder
      builder.subbuilders.map(_._1)
    }
  }

  def main(args: Array[String]) = {
    Command.setExitMode(Command.ExitMode.throwException)
    Repl.runLoop(
      { args =>
        if (args.length == 1 && args(0) == ":help") {
          allApps.foreach { case (name, cmdClass) =>
            println(s"${name.padRight(12)}${cmdClass.banner}")
          }
        } else if (args.length > 1) {
          allApps.get(args(0)) match {
            case Some(cmdClass) => {
              try {
                cmdClass.runCommand(args.drop(1))
              } catch {
                case _: ExitException =>
                case e: Exception     => println(e)
              }
            }
            case None =>
          }
        }
        println()
        true
      },
      { builder =>
        val completers = allApps.map { case (name, cmdClass) =>
          new AggregateCompleter(
            new ArgumentCompleter(
              new StringsCompleter(name),
              new StringsCompleter(cmdClass.subCommandNames: _*),
              new StringsCompleter("--help"),
              NullCompleter.INSTANCE
            ),
            new ArgumentCompleter(
              new StringsCompleter(name),
              new StringsCompleter("--help"),
              NullCompleter.INSTANCE
            )
          )
        }.toSeq ++ Seq(
          new ArgumentCompleter(
            new StringsCompleter(":help"),
            NullCompleter.INSTANCE
          ),
          new ArgumentCompleter(
            new StringsCompleter(":quit"),
            NullCompleter.INSTANCE
          )
        )
        builder.completer(new AggregateCompleter(completers: _*))
      }
    )
  }
}

object allApps
  extends BiMap[String, Class[_ <: Command]](
    "audit" -> classOf[Audit],
    "auth" -> classOf[Auth],
    "cert" -> classOf[Cert],
    "compare" -> classOf[Compare],
    "dataverify" -> classOf[Verify],
    "delegate" -> classOf[Delegate],
    "lookback" -> classOf[Lookback],
    "payment" -> classOf[Payment],
    "query" -> classOf[Query],
    "treatment" -> classOf[Treatment]
  )
