ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "br.dev.contrib.de"

lazy val de = project
  .in(file("."))
  .settings(
    name := "DataSusSparkJobs",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.0",
      "org.apache.spark" %% "spark-sql" % "3.5.0",
      "br.dev.contrib.gov.sus.opendata" % "libdatasus-parquet-dbf" % "1.0.1",
      "com.google.cloud.spark" %% "spark-bigquery" % "0.35.1",
      "com.google.cloud" % "google-cloud-bigquery" % "2.37.0"
    ),

  )
//
//assemblyShadeRules in assembly := Seq(
//  ShadeRule.rename("com.google.common.**" -> "repackaged.com.google.common.@1").inAll,
//  ShadeRule.rename("com.google.protobuf.**" -> "repackaged.com.google.protobuf.@1").inAll
//)
