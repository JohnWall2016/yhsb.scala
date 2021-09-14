package yhsb.base.io

import java.lang.System
import java.nio.file.Paths

import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.impl.DefaultParser
import org.jline.reader.impl.history.DefaultHistory
import org.jline.terminal.TerminalBuilder
import yhsb.base.text.Parser

object Repl {
  def runLoop(action: Seq[String] => Boolean) = {
    var args: Seq[String] = null

    val parser = new DefaultParser()
    parser.setEscapeChars(null)

    val terminal = TerminalBuilder
      .builder()
      .system(true)
      .build()

    val reader = LineReaderBuilder
      .builder()
      .terminal(terminal)
      .parser(parser)
      .history(new DefaultHistory)
      .variable(
        LineReader.HISTORY_FILE,
        Paths.get(System.getProperty("user.home"), ".yhsb")
      )
      .build()

    var continue = true
    do {
      val line = reader.readLine("> ")
      args = Parser.parseLine(line)
      continue = action(args) &&
        !(args.length == 1 && (args(0) == ":q" || args(0) == ":quit"))
    } while (continue)
  }
}
