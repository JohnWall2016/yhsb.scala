import mill._, scalalib._
import coursier.maven.MavenRepository

trait CustomModule extends ScalaModule {
  def scalaVersion = "2.13.3"

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
      ivy"io.getquill:quill-jdbc_2.13:3.5.2",
      ivy"org.apache.logging.log4j:log4j-slf4j-impl:2.13.3",
      ivy"org.rogach:scallop_2.13:3.5.0",
      ivy"com.google.code.gson:gson:2.8.6",
      ivy"com.typesafe:config:1.4.0"
    )

  object app extends CustomModule {
    object cjb extends CustomModule {
      object fullcover extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }

      object audit extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }

      object query extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }

      object dataverify extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }

      object cert extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }

      object payment extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }

      object delegate extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }
    }

    object qb extends CustomModule {
      object spancalc extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }

      object landacq extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }
    }

    object jgb extends CustomModule {
      object clearpilot extends CustomModule {
        override def moduleDeps = Seq(yhsb)
      }
    }
  }

  object test extends Tests {
    //def moduleDeps = Seq(yhsb)
    override def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.1")
    def testFrameworks = Seq("utest.runner.Framework")
  }
}
