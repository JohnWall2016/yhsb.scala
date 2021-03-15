package yhsb.base.util

import scala.util.matching.Regex

object Checksum {
  def idcardChecksum(idcard: String): Option[Char] = {
    val regex = """^(\d){17}""".r
    regex.findPrefixOf(idcard) match {
      case None => None
      case Some(chars) =>
        val index =
          chars
            .map(_ - '0')
            .zip(Seq(7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2))
            .foldLeft(0)((sum, pair) => sum + pair._1 * pair._2) % 11
        Some(Seq('1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2')(index))
    }
  }
}
