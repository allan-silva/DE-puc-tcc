package br.dev.contrib.gov.sus.opendata.jobs

import org.apache.hadoop.conf.Configuration
import org.apache.spark.sql.{SaveMode, SparkSession}
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import java.time.LocalDate

object DatasusFileDiscoveryMetadataJob {

  val DISCOVERY_DATASET = "ingestion_info"
  val DISCOVERY_TABLE = "discovered_files"

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("Datasus file discovery")
      .getOrCreate

    import spark.implicits._

    if (args.length < 2) {
      throw new IllegalArgumentException("No enough arguments")
    }

    val inputDir = args(0)
    val sourceSystem = args(1)

    val files = listFiles(inputDir, spark.sparkContext.hadoopConfiguration)
      .map(path => (path.toUri.toString, sourceSystem, LocalDate.now()))
      .toSeq
      .toDF("file_uri", "source", "discovery_date")
    files
      .createOrReplaceTempView("scanned_files")

    val discoveredFilesDF = spark.read
      .format("bigquery")
      .option("dataset", DISCOVERY_DATASET)
      .option("table", DISCOVERY_TABLE)
      .load()
    discoveredFilesDF
      .createOrReplaceTempView("discovered_files")

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

    newFiles
      .write
          .format("bigquery")
          .mode(SaveMode.Append)
          .option("writeMethod", "direct")
          .option("dataset", DISCOVERY_DATASET)
          .option("table", DISCOVERY_TABLE)
          .save()
  }

  private def listFiles(input: String, conf: Configuration): Set[Path] = {
    var files = Set.empty[Path]
    val fsIter = FileSystem.get(conf).listFiles(new Path(input), false)
    while (fsIter.hasNext) {
      val locatedStatus = fsIter.next()
      if (locatedStatus.isFile) files += locatedStatus.getPath
    }
    files
  }

}

// spark-submit --class br.dev.contrib.gov.sus.opendata.jobs.DatasusFileDiscoveryMetadataJob --master spark://allan-ThinkPad-E14-Gen-2:7077 --packages "com.google.cloud.spark:spark-bigquery-with-dependencies_2.13:0.36.1,com.google.cloud:google-cloud-bigquery:2.37.0" --conf "spark.executor.userClassPathFirst=true" --conf "spark.driver.userClassPathFirst=true" --conf "credentialsFile=/home/allan/secdrop/puc-tcc-412315-9e63f609ce1f.json" /home/allan/code.allan/DE-puc-tcc/spark/target/scala-2.13/datasussparkjobs_2.13-0.1.0-SNAPSHOT.jar /home/allan/teste/inputDirectory ingestion_info
