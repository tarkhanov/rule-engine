
name := "client"

scalaJSUseMainModuleInitializer := false

libraryDependencies ++= Seq(
  "org.scala-js" %%% "scalajs-dom" % "0.9.1",
  "com.lihaoyi" %%% "scalatags" % "0.6.7",
  "org.querki" %%% "jquery-facade" % "1.0"
)

enablePlugins(ScalaJSPlugin, ScalaJSWeb)