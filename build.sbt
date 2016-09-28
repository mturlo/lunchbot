name := "lunchbot"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.8"

libraryDependencies ++= {

  object Versions {
    val slime = "0.2.0-SNAPSHOT"

    val scalatest = "3.0.0"
  }

  Seq(
    "com.cyberdolphins" %% "slime" % Versions.slime
  ) ++
  Seq(
    "org.scalatest" %% "scalatest" % Versions.scalatest % "test"
  )

}

lazy val root = (project in file("."))