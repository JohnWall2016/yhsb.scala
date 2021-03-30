package yhsb

import utest.{TestSuite, Tests, test}
import yhsb.base.reflect.Extension._
import scala.reflect.runtime.universe._
import scala.annotation.StaticAnnotation
import scala.annotation.meta._
import scala.beans.BeanProperty

class Human(@Description(value = "Sex") val sex: String)

trait Age { var age: Int }

case class Additional(occupation: String, education: String)

@field @getter @setter
case class Description(value: String) extends StaticAnnotation

@Description("Person")
class Person[T >: Null](@Description(value = "Name") var name: String, var age: Int, sex: String)
  extends Human(sex)
     with Age {
  @Description(value = "Additional")
  var additional: T = null

  override def toString(): String = s"Person($name, $age, $sex, $additional)"
}

object ReflectiveTest extends TestSuite {
  override def tests: Tests =
    Tests {
      test("reflect") {
        val p = new Person("John", 41, "male")

        println(p.annotations)

        println("=" * 60)
        p.getSetters.foreach(p => println(s"${p._1} -> ${p._2}"))
        println("=" * 60)
        p.getGetters.foreach(p => println(s"${p._1} -> ${p._2}"))

        println("=" * 60)
        val p2 = newInstance[Person[Additional]]("Peter", 43, "male")
        println(p2)
        p2.getGetters.foreach { case (name, info) =>
          println(s"$name=${info.method()}")
        }

        println("=" * 60)
        val map = Map(
          "name" -> "Berry",
          "age" -> 20,
          "sex" -> "female",
          "additional" -> Additional("Software Engineer", "BA")
        )
        val setters = p2.getSetters
        map.foreach { case (key, value) =>
          setters.get(key) match {
            case Some(info) => info.method(value)
            case None       =>
          }
        }
        println(p2)
      }
    }
}
