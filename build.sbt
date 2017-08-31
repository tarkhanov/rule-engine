name := """rule-engine"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

resolvers += Resolver.sonatypeRepo("snapshots")

scalaVersion := "2.12.3" // "2.11.11"

libraryDependencies += guice
libraryDependencies += "commons-io" % "commons-io" % "2.4"

libraryDependencies += "org.xerial" % "sqlite-jdbc" % "3.20.0"
libraryDependencies += "org.hashids" % "hashids" % "1.0.3"

libraryDependencies += "org.python" % "jython-standalone" % "2.7.0"

libraryDependencies += "com.mohiva" %% "play-html-compressor" % "0.7.1"

libraryDependencies += "com.typesafe.play" %% "play-iteratees" % "2.6.1"
libraryDependencies += "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"
libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += "com.typesafe.play" %% "play-slick" %  "3.0.1"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.4" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.10.19" % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test
