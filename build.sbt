name := """git-stats-backend"""
organization := "net.imadz"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.8"

libraryDependencies += evolutions
libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.2" % Test
libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-slick" % "3.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0",
  "com.typesafe.slick" %% "slick" % "3.2.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.2.1",
  "com.typesafe.slick" %% "slick-codegen" % "3.2.1",
  "mysql" % "mysql-connector-java" % "5.1.34"

)
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "net.imadz.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "net.imadz.binders._"
import com.typesafe.sbt.packager.docker._

dockerCommands := Seq(
  Cmd("FROM", "openjdk:8-jre-alpine"),
  Cmd("RUN", """apk update && apk upgrade && \
                   apk add --no-cache bash git openssh"""),
  Cmd("WORKDIR", "/opt/docker"),
  Cmd("ADD", "--chown=daemon:daemon opt /opt"),
  Cmd("CMD", """["bin/git-stats-backend", "-v", "-Dpidfile.path=/dev/null", "-Dplay.http.secret.key=ad31779d4ee49d5ad5162bf1429c32e2e9933f3b"]"""),
  Cmd("EXPOSE", """9000""")
)

dockerExposedPorts ++= Seq(9000, 9001)

mappings in Universal ++=
  (baseDirectory.value / "scripts" * "*" get) map
    (x => x -> ("" + x.getName))
