
// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.7")

// ScalaJS
resolvers += Resolver.bintrayRepo("vmunier", "scalajs")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.20")
addSbtPlugin("com.vmunier" % "sbt-web-scalajs" % "1.0.6")

//addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "1.0.1") All CoffeeScripts were rewritten in ScalaJS
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")
//addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "1.0.5")
//addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "1.0.9")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.3")
//addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.1")


