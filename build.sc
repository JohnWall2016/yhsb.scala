import mill._, scalalib._
import coursier.maven.MavenRepository

trait CustomModule extends ScalaModule {
  def scalaVersion = "2.13.3"

  def scalacOptions = Seq(
    "-deprecation",
    "-feature",
    "-language:implicitConversions",
    "-language:reflectiveCalls",
    "-language:existentials"
  )

  override def repositoriesTask =
    T.task {
      super.repositoriesTask() ++ Seq(
        MavenRepository(
          "https://oss.sonatype.org/content/repositories/releases"
        )
      )
    }
}

object yhsb extends CustomModule {
  override def ivyDeps =
    Agg(
      ivy"org.apache.poi:poi:4.1.2",
      ivy"org.apache.poi:poi-ooxml:4.1.2",
      ivy"mysql:mysql-connector-java:8.0.17",
      ivy"org.xerial:sqlite-jdbc:3.28.0",
      ivy"io.getquill:quill-jdbc_2.13:3.5.2",
      ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.13.3",
      ivy"org.rogach:scallop_2.13:3.5.0",
      ivy"com.google.code.gson:gson:2.8.6",
      ivy"com.typesafe:config:1.4.0",
      ivy"org.scala-lang:scala-compiler:2.13.3",
      ivy"org.zeroturnaround:zt-zip:1.14",
      ivy"axis:axis:1.4",
      ivy"org.jline:jline:3.1.3",
    )
    
  trait YhsbModule extends CustomModule {
    override def moduleDeps = Seq(yhsb)
  }

  object app extends CustomModule {
    object cjb extends CustomModule {
      object fullcover extends YhsbModule
      object audit extends YhsbModule
      object query extends YhsbModule
      object dataverify extends YhsbModule
      object cert extends YhsbModule
      object payment extends YhsbModule
      object delegate extends YhsbModule
      object treatment extends YhsbModule
      object auth extends YhsbModule
      object compare extends YhsbModule
      object lookback extends YhsbModule
    }
    object qb extends CustomModule {
      object spancalc extends YhsbModule
      object landacq extends YhsbModule
      object query extends YhsbModule
    }
    object jgb extends CustomModule {
      object clearpilot extends YhsbModule
    }
  }

  object test extends Tests {
    //def moduleDeps = Seq(yhsb)
    override def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.1")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
