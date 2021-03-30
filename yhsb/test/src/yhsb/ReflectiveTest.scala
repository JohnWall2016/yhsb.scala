package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.reflective.Extension._
import scala.reflect.runtime.universe._

class Human(val sex: String)
trait Age { var age: Int }
class Person(var name: String, var age: Int, sex: String) extends Human(sex) with Age

object ReflectiveTest extends TestSuite {
  override def tests: Tests =
    Tests {
      test("reflect") {
        val p = new Person("John", 41, "male")
        println(p.getSetters)
        println(p.getGetters)
      }
    }
}