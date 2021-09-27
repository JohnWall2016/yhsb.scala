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

case class Repl(
    action: Seq[String] => Boolean,
    initialize: Repl => Unit = { _ => },
    finalize_ : Repl => Unit = { _ => }
) {
  var prompt: String = ""

  val parser = {
    val p = new DefaultParser
    p.setEscapeChars(null)
    p
  }

  val terminal = TerminalBuilder
    .builder()
    .system(true)
    .build()

  val history = new DefaultHistory

  val readerBuilder = {
    val rb = LineReaderBuilder.builder()
    rb
      .terminal(terminal)
      .parser(parser)
      .history(history)
      .variable(
        LineReader.HISTORY_FILE,
        Paths.get(System.getProperty("user.home"), ".yhsb_history")
      )
    rb
  }

  def runLoop() = {
    try {
      initialize(this)
      val reader = readerBuilder.build()
      try {
        var continue = true
        while (continue) {
          try {
            val line = reader.readLine(s"${prompt}> ")
            val args = Parser.parseLine(line)
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
      finalize_(this)
    }
  }
}
