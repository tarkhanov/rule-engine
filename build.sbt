import sbt.Keys._

val projectScalaVersion = "2.11.7"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

lazy val root = (project in file("."))
  .settings(
    name := "rule-engine",
    version := "1.0",
    scalaVersion := projectScalaVersion,

    libraryDependencies ++= Seq(
        jdbc,
        cache,
        "commons-io" % "commons-io" % "2.4",
        "mysql" % "mysql-connector-java" % "5.1.32",
        "org.xerial" % "sqlite-jdbc" % "3.20.0",
        "com.timesprint" %% "hashids-scala" % "1.0.0",
        "org.python" % "jython-standalone" % "2.7.0",
        "com.mohiva" %% "play-html-compressor" % "0.5.0",
        "com.typesafe.play" %% "play-slick" %  "1.1.1",
        "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
    )
  )
  .enablePlugins(PlayScala)


