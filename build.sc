import mill._, scalalib._
import coursier.maven.MavenRepository

object CustomZincWorkerModule extends ZincWorkerModule {
  def repositories() = super.repositories ++ Seq(
    MavenRepository("https://maven.aliyun.com/repository/central"),
    MavenRepository("https://maven.aliyun.com/repository/public"),
  )
}

object yhsb extends ScalaModule {
  def scalaVersion = "2.13.3"
  def zincWorker = CustomZincWorkerModule

  def ivyDeps = Agg(
    ivy"org.apache.poi:poi:4.1.2",
    ivy"org.apache.poi:poi-ooxml:4.1.2",
    ivy"mysql:mysql-connector-java:8.0.17",
    ivy"io.getquill:quill-jdbc_2.13:3.5.2",
    ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.13.3",
    ivy"org.rogach:scallop_2.13:3.5.0",
    ivy"com.google.code.gson:gson:2.8.6",
    ivy"com.typesafe:config:1.4.0",
  )

  object app extends ScalaModule {
    def scalaVersion = "2.13.3"

    object cjb extends ScalaModule {
      def scalaVersion = "2.13.3"

      object fullcover extends ScalaModule {
        def moduleDeps = Seq(yhsb)
        def scalaVersion = "2.13.3"
      }

      object audit extends ScalaModule {
        def moduleDeps = Seq(yhsb)
        def scalaVersion = "2.13.3"
      }

      object query extends ScalaModule {
        def moduleDeps = Seq(yhsb)
        def scalaVersion = "2.13.3"
      }

      object dataverify extends ScalaModule {
        def moduleDeps = Seq(yhsb)
        def scalaVersion = "2.13.3"
      }
    }

    object qb extends ScalaModule {
      def scalaVersion = "2.13.3"

      object spancalc extends ScalaModule {
        def moduleDeps = Seq(yhsb)
        def scalaVersion = "2.13.3"
      }

      object landacq extends ScalaModule {
        def moduleDeps = Seq(yhsb)
        def scalaVersion = "2.13.3"
      }
    }

    object jgb extends ScalaModule {
      def scalaVersion = "2.13.3"

      object clearpilot extends ScalaModule {
        def moduleDeps = Seq(yhsb)
        def scalaVersion = "2.13.3"
      }
    }
  }

  object test extends Tests {
    //def moduleDeps = Seq(yhsb)
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.1")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}