import com.typesafe.sbt.web.Import.pipelineStages
import sbt.Keys.{compile, libraryDependencies}

name := """rule-engine"""

version := "1.0"

scalaVersion := "2.12.3"

lazy val client = (project in file("client"))
  .settings (
    scalaVersion := scalaVersion.value
  )

lazy val root = (project in file("."))
  .settings(
    name := name.value,
    version := version.value,
    scalaVersion := scalaVersion.value,
    scalaJSProjects := Seq(client),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    libraryDependencies ++= Seq(
      "com.vmunier" %% "scalajs-scripts" % "1.1.1",

      guice,
      "commons-io" % "commons-io" % "2.4",

      "org.xerial" % "sqlite-jdbc" % "3.20.0",
      "org.hashids" % "hashids" % "1.0.3",

      "org.python" % "jython-standalone" % "2.7.0",

     "com.mohiva" %% "play-html-compressor" % "0.7.1",

      "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1",
      "com.typesafe.play" %% "play-json" % "2.6.0",
      "com.typesafe.play" %% "play-slick" %  "3.0.1",

      "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",

      "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % Test,
      "org.mockito" % "mockito-all" % "1.10.19" % Test,
      "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
    )
).enablePlugins(PlayScala)


