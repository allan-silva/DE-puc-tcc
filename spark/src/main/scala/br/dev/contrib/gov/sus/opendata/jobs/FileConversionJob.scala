package br.dev.contrib.gov.sus.opendata.jobs

import br.dev.contrib.gov.sus.opendata.jobs.Datasets.{
  CURATED_FILES_SCHEMA,
  CURATED_FILES_TABLE,
  INGESTION_INFO_DATASET
}
import br.gov.sus.opendata.dbf.parquet.DbfParquet
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.hadoop.fs.Path
import org.apache.log4j.Logger
import org.apache.spark.sql._
import org.apache.spark.util.SerializableConfiguration

import java.net.URI
import java.nio.file.Files
import java.sql.Date

object FileConversionJob {
  private val log: Logger = Logger.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      throw new IllegalArgumentException("No enough arguments")
    }

    val spark = SparkSession
      .builder()
      .appName("DBF/DBC file conversion")
      .getOrCreate
      .configureForJob()

    spark.sparkContext.getConf.getAll

    val sourceSystem = args(0)
    val outputBucket = args(1)
    val workerInstances = args(2).toInt

    val notCuratedFiles = spark.notCuratedFilesDF(sourceSystem, workerInstances)
    processFiles(
      spark,
      notCuratedFiles,
      sourceSystem,
      outputBucket,
      workerInstances
    )
  }

  private def processFiles(
      spark: SparkSession,
      notCuratedFiles: DataFrame,
      sourceSystem: String,
      outputBucket: String,
      workerInstances: Int
  ): Unit = {

    if (notCuratedFiles.count() == 0) {
      return
    }

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

    log.info("Successful write to BigQuery")

    processFiles(
      spark,
      spark.notCuratedFilesDF(sourceSystem, workerInstances),
      sourceSystem,
      outputBucket,
      workerInstances
    )
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

    if (!FileOps.exists(convertedFileURI, configuration.value)) {
      log.info(
        s"Converting file: ${localFileUri.toString} => ${convertedFileURI.toString}"
      )
      val converter =
        DbfParquet.builder().withHadoopConf(configuration.value).build()
      converter.convert(
        localFilepath,
        convertedFilePath
      )
    } else {
      log.info(s"${convertedFileURI.toString} already exists, no conversion was made.")
    }

    log.info(
      s"Coping local file to remote: ${convertedFileURI.toString} to ${remoteConvertedFileUri.toString}"
    )
    FileOps.copyOverwriting(
      convertedFileURI,
      remoteConvertedFileUri,
      configuration.value
    )

    Row(
      remoteFileURI.toString,
      remoteConvertedFileUri.toString,
      row.getAs[String]("source"),
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
      spark.conf.set("cacheExpirationTimeInMinutes", 0)
      spark.conf.set("materializationDataset", INGESTION_INFO_DATASET)
      spark
    }

    def notCuratedFilesDF(
        sourceSystem: String,
        workerInstances: Int
    ): DataFrame = {
      // Current version of bigquery connector does not support sql parameters to prevent sql injection
      // Once `sourceSystem`is provided by dataproc workflow template, not a problem here.

      val sql =
        s"""
            WITH converted_files AS(
              SELECT
                source_file_uri
              FROM
                `$INGESTION_INFO_DATASET.curated_files`
              WHERE
                success IS TRUE
            )
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
              (c.source_file_uri IS NULL OR c.success IS FALSE)
              AND
              d.file_uri NOT IN(
                SELECT * from converted_files
              ) LIMIT $workerInstances
        """

      spark.read
        .format("bigquery")
        .load(sql)
    }
  }
}
