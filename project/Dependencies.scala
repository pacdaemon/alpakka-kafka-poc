import sbt._

object Versions {
  lazy val akkaVersion = "2.6.9"
}

object Dependencies {
  import Versions._
  lazy val akkaTypedActors        = "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion
  lazy val akkaTypedActorsTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion
  lazy val akkaStream             = "com.typesafe.akka" %% "akka-stream"              % akkaVersion
  lazy val akkaStreamTestkit      = "com.typesafe.akka" %% "akka-stream-testkit"      % akkaVersion
  lazy val akkaStreamKafka        = "com.typesafe.akka" %% "akka-stream-kafka"        % "2.0.5"
  lazy val cats                   = "org.typelevel"     %% "cats-core"                % "2.1.1"
  lazy val logbackClassic         = "ch.qos.logback"    % "logback-classic"           % "1.2.3"
  lazy val scalaCheck             = "org.scalacheck"    %% "scalacheck"               % "1.14.3"
  lazy val scalaTest              = "org.scalatest"     %% "scalatest"                % "3.2.2"
  lazy val scalaTestPlus          = "org.scalatestplus" %% "scalacheck-1-14"          % "3.2.0.0"
}
