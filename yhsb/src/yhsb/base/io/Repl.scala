package yhsb.base.io

import java.lang.System
import java.nio.file.Paths

import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder

import yhsb.base.text.Parser

//Windows: Console Emulator

object Repl {
  def runLoop(
      action: Seq[String] => Boolean,
      initialize: LineReaderBuilder => Unit = { b => },
      finalize: => Unit = {}
  ) = {
    try {
      var args: Seq[String] = null

      val parser = new DefaultParser
      parser.setEscapeChars(null)

      val terminal = TerminalBuilder
        .builder()
        .system(true)
        .build()

      val history = new DefaultHistory

      val readerBuilder = LineReaderBuilder.builder()

      readerBuilder
        .terminal(terminal)
        .parser(parser)
        .history(history)
        .variable(
          LineReader.HISTORY_FILE,
          Paths.get(System.getProperty("user.home"), ".yhsb_history")
        )

      initialize(readerBuilder)

      val reader = readerBuilder.build()

      try {
        var continue = true
        while (continue) {
          try {
            val line = reader.readLine("> ")
            args = Parser.parseLine(line)
            continue = action(args) &&
              !(args.length == 1 && (args(0) == ":q" || args(0) == ":quit"))
          } catch {
            case _: UserInterruptException =>
          }
        }
      } catch {
        case _: EndOfFileException =>
          history.save()
          println("Byte!")
      }
    } finally {
      finalize
    }
  }
}
