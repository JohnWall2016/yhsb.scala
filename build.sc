import mill._, scalalib._
import coursier.maven.MavenRepository

object CustomZincWorkerModule extends ZincWorkerModule {
  def repositories() = super.repositories ++ Seq(
    MavenRepository("https://maven.aliyun.com/repository/public")
  )
}

object yhsb extends ScalaModule {
  def scalaVersion = "2.13.1"
  def zincWorker = CustomZincWorkerModule

  object cjb extends ScalaModule {
    def moduleDeps = Seq(yhsb)
    def scalaVersion = "2.13.1"
  }

  object test extends Tests {
    //def moduleDeps = Seq(yhsb)
    def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.1")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}