name := "lunchbot"

version := "0.6.0-SNAPSHOT"

scalaVersion := "2.11.8"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= {

  object Versions {
    val slackScalaClient = "0.2.0"
    val akka = "2.4.16"
    val ficus = "1.2.3"

    val scalatest = "3.0.0"
    val mockito = "2.2.3"
  }

  Seq(
    "com.github.gilbertw1" %% "slack-scala-client" % Versions.slackScalaClient,
    "com.iheart" %% "ficus" % Versions.ficus,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
    "ch.qos.logback" % "logback-classic" % "1.1.7",
    "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
    "com.typesafe.akka" %% "akka-persistence-query-experimental" % Versions.akka,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.4.16.0",
    "org.iq80.leveldb" % "leveldb" % "0.7",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
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
  case PathList("META-INF", xs @ _*) =>
    xs match {
      case "native" +: _ => MergeStrategy.first
      case _ => MergeStrategy.discard
    }
  case PathList("reference.conf") =>
    MergeStrategy.concat
  case _ =>
    MergeStrategy.first
}

lazy val root = project in file(".")

// scalastyle check on compile

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compile in Compile) <<= (compile in Compile) dependsOn compileScalastyle
