name := "spark-assignment2"

version := "0.1"

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.5.1",
  "org.apache.spark" %% "spark-sql"  % "3.5.1",
  "org.apache.spark" %% "spark-mllib" % "3.5.1",
  "org.slf4j" % "slf4j-log4j12" % "1.7.36",
  "org.slf4j" % "slf4j-api" % "1.7.36"
)
