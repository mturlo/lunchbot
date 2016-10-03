name := "lunchbot"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= {

  object Versions {
    val slackScalaClient = "0.1.8"
    val akka = "2.4.4"

    val scalatest = "3.0.0"
  }

  Seq(
    "com.github.gilbertw1" %% "slack-scala-client" % Versions.slackScalaClient
  ) ++
    Seq(
      "org.scalatest" %% "scalatest" % Versions.scalatest % "test",
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test"
    )

}

scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-language:postfixOps")

mainClass in assembly := Some("Main")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.first
}

lazy val root = project in file(".")