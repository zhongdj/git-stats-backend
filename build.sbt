import slick.codegen.SourceCodeGenerator
import slick.{model => m}
import scalariform.formatter.preferences._
import com.typesafe.sbt.packager.docker._

name := """git-stats-backend"""
organization := "net.imadz"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(CodegenPlugin)
  .settings(slickCodegenSettings: _*)
  .settings(
    scalaVersion := "2.12.8",
    libraryDependencies += evolutions,
    libraryDependencies += guice,
    libraryDependencies += ws,
    libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test,
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-slick" % "3.0.0",
      "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
      "com.typesafe.slick" %% "slick" % "3.2.1",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
      "com.typesafe.slick" %% "slick-codegen" % "3.2.1",
      "dev.zio" %% "zio" % "1.0.7",
      "mysql" % "mysql-connector-java" % "5.1.34"
    ),
    // Adds additional packages into Twirl
    //TwirlKeys.templateImports += "net.imadz.controllers._"

    // Adds additional packages into conf/routes
    // play.sbt.routes.RoutesKeys.routesImport += "net.imadz.binders._"

    dockerCommands := Seq(
      Cmd("FROM", "openjdk:8-jre-alpine"),
      Cmd("RUN",
        """apk update && apk upgrade && \
                   apk add --no-cache bash git openssh docker"""),
      Cmd("WORKDIR", "/opt/docker"),
      Cmd("ADD", "--chown=daemon:daemon opt /opt"),
      Cmd("CMD", """["bin/git-stats-backend", "-v", "-Dpidfile.path=/dev/null", "-Dplay.http.secret.key=ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"]"""),
      Cmd("EXPOSE", """9000""")
    ),

    dockerExposedPorts ++= Seq(9000, 9001),

    mappings in Universal ++=
      (baseDirectory.value / "scripts" * "*" get) map
        (x => x -> ("" + x.getName)),

    scalariformPreferences := scalariformPreferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentConstructorArguments, true)
      .setPreference(DanglingCloseParenthesis, Preserve),
    slickCodegenDatabaseUrl := "jdbc:mysql://127.0.0.1:3307/stats?noAccessToProcedureBodies=true&createDatabaseIfNotExist=true&m=yes&characterEncoding=UTF-8&connectTimeout=300000&useSSL=false&socketTimeout=30000&autoReconnect=true&maxReconnects=10&initialTimeout=10",
    slickCodegenDatabaseUser := "root",
    slickCodegenDatabasePassword := "1q2w3e4r5t",
    slickCodegenDriver := slick.jdbc.MySQLProfile,
    slickCodegenJdbcDriver := "com.mysql.jdbc.Driver",
    slickCodegenOutputPackage := "net.imadz.git.stats.models",
    slickCodegenExcludedTables := Seq("play_evolutions"),
    slickCodegenCodeGenerator := { (model:  m.Model) =>
      new SourceCodeGenerator(model) {
        override def code =
         "import org.joda.time.DateTime\n" + super.code
        override def Table = new Table(_) {
          override def Column = new Column(_) {
            override def rawType = model.tpe match {
              case "java.sql.Timestamp" => "DateTime" // kill j.s.Timestamp
              case _ =>
                super.rawType
            }
          }
        }
      }
    },
    slickCodegenOutputDir := (sourceManaged in Compile).value / "slickCodegen",
    sourceGenerators in Compile += slickCodegen
  )
