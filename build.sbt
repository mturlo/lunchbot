name := "lunchbot"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= {

  object Versions {
    val slackScalaClient = "0.1.8"

    val scalatest = "3.0.0"
  }

  Seq(
    "com.github.gilbertw1" %% "slack-scala-client" % Versions.slackScalaClient
  ) ++
    Seq(
      "org.scalatest" %% "scalatest" % Versions.scalatest % "test"
    )

}

lazy val root = project in file(".")