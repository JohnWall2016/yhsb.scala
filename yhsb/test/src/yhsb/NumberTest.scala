package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.math.Numbers.BigDecimalOps

object NumberTest extends TestSuite {
  override def tests: Tests =
    Tests {
      test("syslogin") {
        println(BigDecimal("989290190.21").toChineseMoney)
      }
    }
}