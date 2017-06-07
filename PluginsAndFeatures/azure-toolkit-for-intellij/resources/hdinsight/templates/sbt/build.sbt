name := "sample"

version := "1.0"

scalaVersion := "2.11.10"

libraryDependencies ++= Seq(
  "org.apache.spark" % "spark-core_2.11" % "2.0.2",
  "org.apache.spark" % "spark-sql_2.11" % "2.0.2",
  "org.apache.spark" % "spark-streaming_2.11" % "2.0.2",
  "org.apache.spark" % "spark-mllib_2.11" % "2.0.2"
)