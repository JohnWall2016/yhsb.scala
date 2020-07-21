import mill._, scalalib._
import coursier.maven.MavenRepository

object CustomZincWorkerModule extends ZincWorkerModule {
  def repositories() = Seq(
    MavenRepository("https://maven.aliyun.com/repository/central"),
    MavenRepository("https://maven.aliyun.com/repository/public"),
  ) ++ super.repositories
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
  )

  object app extends ScalaModule {
    def scalaVersion = "2.13.3"

    object cjb extends ScalaModule {
      def scalaVersion = "2.13.3"

      object fullcover extends ScalaModule {
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