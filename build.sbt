name := "kafka-akka-stream-quill-postgres"
organization := "com.streese"

version := "0.1.0"
scalaVersion := "2.13.3"

enablePlugins(BuildInfoPlugin)

scalacOptions ++= List(
  "-Ywarn-unused"
)

libraryDependencies ++= Seq(
  "ch.qos.logback"    %  "logback-classic"   % "1.2.3"          ,
  "com.typesafe.akka" %% "akka-stream"       % "2.6.10"         ,
  "com.typesafe.akka" %% "akka-stream-kafka" % "2.0.5"          ,
  "io.getquill"       %% "quill-jdbc"        % "3.5.3"          ,
  "org.flywaydb"      %  "flyway-core"       % "7.0.4"          ,
  "org.postgresql"    %  "postgresql"        % "42.2.8"         ,
  "org.scalatest"     %% "scalatest"         % "3.2.2"  % "test"
)

buildInfoPackage := "com.streese"
buildInfoKeys := Seq[BuildInfoKey](name)
