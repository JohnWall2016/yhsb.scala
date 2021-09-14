package yhsb.base.text

import scala.collection.mutable.ArrayBuffer

object Parser {
  def parseLine(line: String): Seq[String] = {
    val args = ArrayBuffer[String]()
    val builder = new StringBuilder()

    var isQuote = false
    var prevChar = ' '
    for (c <- line) {
      if (c == '"') {
        if (!isQuote) {
          if (builder.nonEmpty) {
            args.addOne(builder.toString())
          }
          isQuote = true
        } else {
          args.addOne(builder.toString())
          isQuote = false
          builder.clear()
        }
      } else if (!isQuote && (c == ' ' || c == '\t')) {
        if (builder.nonEmpty) {
          args.addOne(builder.toString())
          builder.clear()
        }
      } else {
        builder.append(c)
      }
    }

    if (builder.nonEmpty) {
      args.addOne(builder.toString())
    }
    args.toSeq
  }
}
