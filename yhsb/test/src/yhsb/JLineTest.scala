package yhsb

import utest.{TestSuite, Tests, test}
import org.jline.reader.impl.DefaultParser
import org.jline.terminal.TerminalBuilder
import org.jline.reader.LineReaderBuilder
import yhsb.base.text.Parser
import org.jline.reader.impl.history.DefaultHistory
import org.jline.reader.LineReader
import java.nio.file.Paths
import java.lang.System

object JLineTest extends TestSuite {
  def tests =
    Tests {
      test("run") {
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

        do {
          val line = reader.readLine("> ")
          args = Parser.parseLine(line)
          terminal.writer().println(args)
        } while (!(args.length == 1 && (args(0) == ":q" || args(0) == ":quit")))
      }
    }
}
