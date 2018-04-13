name := "simscala"

version := "0.1"

scalaVersion := "2.12.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.11",
   "com.markatta" %% "timeforscala" % "1.6-SNAPSHOT",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.11" % Test
)
