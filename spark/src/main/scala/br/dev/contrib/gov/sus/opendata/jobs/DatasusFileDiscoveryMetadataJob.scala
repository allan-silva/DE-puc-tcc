package br.dev.contrib.gov.sus.opendata.jobs

import br.dev.contrib.gov.sus.opendata.jobs.Datasets.{INGESTION_INFO_DATASET, DISCOVERY_TABLE}
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}
import org.apache.hadoop.fs.Path
import org.apache.spark.sql.types.StructType

import java.time.LocalDate
import collection.JavaConverters._

object DatasusFileDiscoveryMetadataJob {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Datasus file discovery")
      .getOrCreate

    println(s"> Program - Scala version: ${scala.util.Properties.versionString}")

    if (args.length < 2) {
      throw new IllegalArgumentException("No enough arguments")
    }

    val inputDir = args(0)
    val sourceSystem = args(1)

    val discoveredFilesDF = spark.discoveredFilesDF()
    discoveredFilesDF
      .createOrReplaceTempView("discovered_files")
    println("> Remote schema")
    discoveredFilesDF.printSchema()

    val scannedFiles = spark.scannedFilesDF(new Path(inputDir), sourceSystem , discoveredFilesDF.schema)
    scannedFiles.createOrReplaceTempView("scanned_files")
    println("> Local schema")
    scannedFiles.printSchema()

    val newFiles = spark.sql(
      """
        | SELECT
        |   SF.file_uri
        |   , SF.source
        |   , SF.discovery_date
        | FROM scanned_files SF
        | LEFT JOIN discovered_files DF
        | ON SF.file_uri = DF.file_uri AND
        |   SF.source = DF.source
        | WHERE
        |   DF.file_uri IS NULL
        |""".stripMargin)

    newFiles.show(false)
    println("> Transformation schema")
    newFiles.printSchema()

    newFiles
      .write
          .format("bigquery")
          .mode(SaveMode.Append)
          .option("writeMethod", "direct")
          .option("dataset", INGESTION_INFO_DATASET)
          .option("table", DISCOVERY_TABLE)
          .save()
  }

  implicit class SparkSessionOps(spark: SparkSession) {
    def scannedFilesDF(inputPath: Path, sourceSystem: String, schema: StructType): DataFrame = {
      val inputFileSystem = inputPath.getFileSystem(spark.sparkContext.hadoopConfiguration)
      val fsIter = inputFileSystem.listFiles(inputPath, false)
      var files = List.empty[String]

      while (fsIter.hasNext) {
        val locatedStatus = fsIter.next()
        if (locatedStatus.isFile) files = locatedStatus.getPath.toUri.toString :: files
      }

      spark.createDataFrame(
        files.map(
          path => Row(path, sourceSystem, LocalDate.now())
        ).asJava,
        schema
      )
    }

    def discoveredFilesDF(): DataFrame = {
      spark.read
        .format("bigquery")
        .option("dataset", INGESTION_INFO_DATASET)
        .option("table", DISCOVERY_TABLE)
        .load()
    }
  }
}
