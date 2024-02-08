package br.dev.contrib.gov.sus.opendata.jobs

import br.dev.contrib.gov.sus.opendata.jobs.Datasets.{CURATED_FILES_SCHEMA, CURATED_FILES_TABLE, INGESTION_INFO_DATASET}
import br.gov.sus.opendata.dbf.parquet.DbfParquet
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.hadoop.fs.Path
import org.apache.log4j.Logger
import org.apache.spark.sql._
import org.apache.spark.util.SerializableConfiguration

import java.net.URI
import java.nio.file.{Files, Paths}
import java.sql.Date

object FileConversionJob {
  private val log: Logger = Logger.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder()
      .appName("DBF/DBC file conversion")
      .getOrCreate
      .configureForJob()

    if (args.length < 2) {
      throw new IllegalArgumentException("No enough arguments")
    }

    val sourceSystem = args(0)
    val outputBucket = args(1)

    val notCuratedFiles = spark.notCuratedFilesDF(sourceSystem)
    notCuratedFiles.show(false)
    log.info("> ^^^ Files to be processed")

    val processedFilesDF = convertFiles(
      notCuratedFiles,
      outputBucket,
      spark.serializedConf()
    )

    processedFilesDF.show(false)
    log.info("> ^^^ Files process result")

    processedFilesDF.write
      .format("bigquery")
      .mode(SaveMode.Append)
      .option("writeMethod", "direct")
      .option("dataset", INGESTION_INFO_DATASET)
      .option("table", CURATED_FILES_TABLE)
      .save()
  }

  private def convertFiles(
      filesDF: DataFrame,
      outputBucket: String,
      configuration: SerializableConfiguration
  ): Dataset[Row] = {
    filesDF.map(row => {
      var mappedRow: Row = null
      try {
        mappedRow = convertFile(row, outputBucket, configuration)
      } catch {
        case e: Exception =>
          val message = e.getMessage + " -- " + ExceptionUtils.getStackTrace(e)
          e.printStackTrace()
          mappedRow = Row(
            row.getAs[String]("file_uri"),
            null,
            row.getAs[String]("source"),
            null,
            false,
            message
          )
      }

      mappedRow
    })(Encoders.row(CURATED_FILES_SCHEMA))

  }

  private def convertFile(
      row: Row,
      outputBucket: String,
      configuration: SerializableConfiguration
  ): Row = {
    val tempDirectory = Files.createTempDirectory("temp-job")

    // Define file paths
    val remoteFileURI = URI.create(row.getAs[String]("file_uri"))
    val remoteFileHadoopPath = new Path(remoteFileURI)

    val localFilepath = tempDirectory.resolve(remoteFileHadoopPath.getName)
    val localFileUri = localFilepath.toUri

    val convertedFilePath =
      tempDirectory.resolve(localFilepath.getFileName + ".parquet")
    val convertedFileURI = convertedFilePath.toUri

    val remoteConvertedFileUri =
      URI.create(s"$outputBucket/${convertedFilePath.getFileName.toString}")

    log.info(
      s"Coping remote file to local: ${remoteFileURI.toString} to ${localFileUri.toString}"
    )

    FileOps.copy(remoteFileURI, localFileUri, configuration.value)

    log.info(
      s"Converting file: ${localFileUri.toString} => ${convertedFileURI.toString}"
    )
    val converter =
      DbfParquet.builder().withHadoopConf(configuration.value).build()
    converter.convert(
      localFilepath,
      convertedFilePath
    )

    log.info(
      s"Coping local file to remote: ${convertedFileURI.toString} to ${remoteConvertedFileUri.toString}"
    )
    FileOps.copyOverwriting(convertedFileURI, remoteConvertedFileUri, configuration.value)

    val sourceSystem = row.getAs[String]("source")
    Row(
      remoteFileURI.toString,
      remoteConvertedFileUri.toString,
      sourceSystem,
      new Date(System.currentTimeMillis()),
      true,
      null
    )
  }

  implicit class SparkSessionOps(spark: SparkSession) {

    def serializedConf() =
      new SerializableConfiguration(spark.sparkContext.hadoopConfiguration)

    def configureForJob(): SparkSession = {
      spark.conf.set("viewsEnabled", true)
      spark.conf.set("materializationDataset", INGESTION_INFO_DATASET)
      spark
    }

    def notCuratedFilesDF(sourceSystem: String): DataFrame = {
      // Current version of bigquery connector does not support sql parameters to prevent sql injection
      // Once `sourceSystem`is provided by dataproc workflow template, not a problem here.

      val sql =
        s"""
          SELECT DISTINCT
            d.file_uri,
            d.source
          FROM
            `$INGESTION_INFO_DATASET.discovered_files` d
          LEFT JOIN
            `$INGESTION_INFO_DATASET.curated_files` c
          ON
            d.file_uri = c.source_file_uri
            AND d.source = c.source
          WHERE
            d.source = '$sourceSystem'
            AND
            (c.source_file_uri IS NULL
            OR c.success = FALSE)
        """

      spark.read
        .format("bigquery")
        .load(sql)
    }
  }
}

// ./sbin/start-master.sh
// ./sbin/start-worker.sh spark://allan-ThinkPad-E14-Gen-2:7077
// spark-submit --class br.dev.contrib.gov.sus.opendata.jobs.FileConversionJob --master spark://allan-ThinkPad-E14-Gen-2:7077 --packages "com.google.cloud.spark:spark-bigquery_2.12:0.36.1,com.google.cloud:google-cloud-bigquery:2.37.0" --conf "spark.executor.userClassPathFirst=true" --conf "spark.driver.userClassPathFirst=true" --conf "credentialsFile=/home/allan/secdrop/puc-tcc-412315-9e63f609ce1f.json" /home/allan/code.allan/DE-puc-tcc/spark/target/scala-2.12/DataSusSparkJobs-assembly-0.1.0-SNAPSHOT.jar SIA /home/allan/teste/outputDirectory

//spark-submit --class br.dev.contrib.gov.sus.opendata.jobs.FileConversionJob \
//  --master spark://allan-ThinkPad-E14-Gen-2:7077 \
//  --packages "com.google.cloud.spark:spark-bigquery-with-dependencies_2.12:0.34.0,br.dev.contrib.gov.sus.opendata:libdatasus-parquet-dbf:1.0.5" \
//    --conf "spark.executor.userClassPathFirst=true" \
//    --conf "spark.driver.userClassPathFirst=true" \
//    --conf "credentialsFile=/home/allan/secdrop/puc-tcc-412315-9e63f609ce1f.json" \
//    /home/allan/code.allan/DE-puc-tcc/spark/target/scala-2.12/datasussparkjobs_2.12-0.1.0-SNAPSHOT.jar SIA file:///home/allan/teste/outputDirectory

// spark-submit --class br.dev.contrib.gov.sus.opendata.jobs.FileConversionJob --master spark://allan-ThinkPad-E14-Gen-2:7077 --packages "com.google.cloud.spark:spark-bigquery-with-dependencies_2.13:0.36.1,com.google.cloud:google-cloud-bigquery:2.37.0" --conf "spark.executor.userClassPathFirst=true" --conf "spark.driver.userClassPathFirst=true" --conf "credentialsFile=/home/allan/secdrop/puc-tcc-412315-9e63f609ce1f.json" /home/allan/code.allan/DE-puc-tcc/spark/target/scala-2.13/datasussparkjobs_2.13-0.1.0-SNAPSHOT.jar SIA /home/allan/teste/outputDirectory
