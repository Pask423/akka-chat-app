name := "akka-chat-app"

organization := "org.pasksoftware"

version := "1.0"

scalaVersion := "2.13.8"

libraryDependencies := Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19",
  "com.typesafe.akka" %% "akka-stream-typed" % "2.6.19",
  "com.typesafe.akka" %% "akka-http" % "10.2.9",
  "de.heikoseeberger" %% "akka-http-circe" % "1.39.2",
  "io.circe" %% "circe-generic" % "0.14.1",
  "com.github.pureconfig" %% "pureconfig" % "0.17.1",
  "ch.qos.logback" % "logback-classic" % "1.2.11",
)