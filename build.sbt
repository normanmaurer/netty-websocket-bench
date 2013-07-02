import scalariform.formatter.preferences._

name := "netty-bench"

organization := "io.wasted"

version := "0.1.0"

mainClass := Some("io.wasted.netty.websocket.bench.Server")

scalaVersion := "2.10.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

resolvers ++= Seq(
  "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
  "wasted.io/repo" at "http://repo.wasted.io/mvn",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "Twitter's Repository" at "http://maven.twttr.com/",
  "Maven Repo" at "http://repo1.maven.org/maven2/",
  "Typesafe Ivy Repo" at "http://repo.typesafe.com/typesafe/ivy-releases",
  "Typesafe Maven Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Java.net Maven2 Repository" at "http://download.java.net/maven/2/"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.6" % "compile",
  "io.netty" % "netty-all" % "4.0.0.CR7",
  "org.javassist" % "javassist" % "3.17.1-GA",
  "joda-time" % "joda-time" % "2.1",
  "org.specs2" %% "specs2" % "1.13" % "test"
)

scalariformSettings

ScalariformKeys.preferences := FormattingPreferences().setPreference(AlignParameters, true)

