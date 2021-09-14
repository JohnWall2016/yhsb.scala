package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.io.Repl

object JLineTest extends TestSuite {
  def tests =
    Tests {
      test("run") {
        Repl.runLoop { args =>
          println(args)
          true
        }
      }
    }
}
