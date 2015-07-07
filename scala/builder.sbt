name := "hello"

version := "1.0"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test",
  "org.scalikejdbc" %% "scalikejdbc"       % "2.2.7" % "test",
  "com.h2database"  %  "h2"                % "1.4.187" % "test",
  "ch.qos.logback"  %  "logback-classic"   % "1.1.3" % "test"
)