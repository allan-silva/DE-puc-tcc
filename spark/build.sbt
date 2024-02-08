ThisBuild / scalaVersion := "2.12.18"
ThisBuild / organization := "br.dev.contrib.de"

lazy val de = project
  .in(file("."))
  .settings(
    name := "DataSusSparkJobs",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.0" % "provided",
      "org.apache.spark" %% "spark-sql" % "3.5.0" % "provided",
      "br.dev.contrib.gov.sus.opendata" % "libdatasus-parquet-dbf" % "1.0.7" % "provided"
    ),
  )
