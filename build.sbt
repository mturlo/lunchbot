name := "lunchbot"

version := "0.5.0-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= {

  object Versions {
    val slackScalaClient = "0.1.8"
    val akka = "2.4.4"
    val ficus = "1.2.3"

    val scalatest = "3.0.0"
    val mockito = "2.2.3"
  }

  Seq(
    "com.github.gilbertw1" %% "slack-scala-client" % Versions.slackScalaClient,
    "com.iheart" %% "ficus" % Versions.ficus,
    "com.h2database" % "h2" % "1.4.193",
    "io.getquill" %% "quill-jdbc" % "1.0.1",
    "com.softwaremill.macwire" %% "macros" % "2.2.5" % "provided"
  ) ++
    Seq(
      "org.scalatest" %% "scalatest" % Versions.scalatest % "test",
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test",
      "org.mockito" % "mockito-core" % Versions.mockito % "test"
    )

}

scalacOptions ++= Seq("-Xfatal-warnings", "-feature", "-language:postfixOps")

mainClass in assembly := Some("Main")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.first
}

lazy val root = project in file(".")

// scalastyle check on compile

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compile in Compile) <<= (compile in Compile) dependsOn compileScalastyle
