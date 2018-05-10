name := "simscala"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka"          %% "akka-actor"     % "2.5.11",
  "com.markatta"               %% "timeforscala"   % "1.6",
  "com.github.julien-truffaut" %% "monocle-core"   % "1.5.0",
  "com.github.julien-truffaut" %% "monocle-macro"  % "1.5.0",
  "ch.qos.logback"             % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging"  % "3.9.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.12",
  "com.typesafe.akka"          %% "akka-testkit"   % "2.5.11" % Test
)
