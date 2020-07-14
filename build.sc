import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import mill._, scalalib._
import coursier.maven.MavenRepository

object CustomZincWorkerModule extends ZincWorkerModule {
  def repositories() = /*super.repositories ++*/ Seq(
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
  )

  object cjb extends ScalaModule {
    def moduleDeps = Seq(yhsb)
    def scalaVersion = "2.13.3"
  }

  object test extends Tests {
    //def moduleDeps = Seq(yhsb)
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.1")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}