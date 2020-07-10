import Dependencies._

name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.13.2"

libraryDependencies ++= Seq(
  akkaTypedActors,
  logbackClassis,
  akkaTypedActorsTestkit % Test,
  scalaCheck             % Test
)
