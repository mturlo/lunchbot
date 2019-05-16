name := "lunchbot"

scalaVersion := "2.12.8"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= {

  object Versions {
    val akka = "2.5.22"
    val macwire = "2.3.2"

    val mockito = "2.2.3"
  }

  Seq(
    "com.github.gilbertw1" %% "slack-scala-client" % "0.2.3",
    "com.iheart" %% "ficus" % "1.4.6",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.akka" %% "akka-persistence" % Versions.akka,
    "com.typesafe.akka" %% "akka-persistence-query" % Versions.akka,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1",
    "org.iq80.leveldb" % "leveldb" % "0.11",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
    "com.softwaremill.macwire" %% "util" % Versions.macwire,
    "com.softwaremill.macwire" %% "macros" % Versions.macwire % "provided",
    "org.scala-lang.modules" %% "scala-async" % "0.10.0" // todo remove
  ) ++
    Seq(
      "org.scalatest" %% "scalatest" % "3.0.7" % "test",
      "com.typesafe.akka" %% "akka-testkit" % Versions.akka % "test",
      "org.mockito" %% "mockito-scala" % "1.4.1" % "test",
    )

}

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-feature",
  "-language:postfixOps",
  "-deprecation"
)

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

assemblyOption in assembly := {
  (assemblyOption in assembly)
    .value
    .copy(prependShellScript = Some(sbtassembly.AssemblyPlugin.defaultShellScript))
}

assemblyJarName in assembly := s"${name.value}-${version.value}"

lazy val root = project in file(".")
