import Dependencies._

name := "alpakka-kafka-poc"

version := "1.0"

scalaVersion := "2.13.2"

libraryDependencies ++= Seq(
  akkaTypedActors,
  logbackClassic,
  akkaStream,
  akkaStreamTestkit % Test,
  akkaStreamKafka,
  akkaTypedActorsTestkit % Test,
  scalaTest              % Test,
  scalaCheck             % Test
)
