package br.dev.contrib.gov.sus.opendata.jobs

import br.dev.contrib.gov.sus.opendata.jobs.Datasets.{
  INGESTION_INFO_DATASET,
  SUS_SYSTEMS_TABLES,
  WAREHOUSE_DATASETS_SCHEMA,
  WAREHOUSE_DATASETS_TABLE
}
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.log4j.Logger
import org.apache.spark.sql._
import org.apache.spark.util.SerializableConfiguration

import java.sql.Date
import scala.collection.JavaConverters._

object ParquetLoadJob {

  private val log: Logger = Logger.getLogger(getClass.getName)

  def main(args: Array[String]): Unit = {
    if (args.length < 1) {
      throw new IllegalArgumentException("No enough arguments")
    }

    val spark = SparkSession
      .builder()
      .appName("Datasus parquet load")
      .getOrCreate
      .configureForJob()

    val sourceSystem = args(0)

    val curatedFilesDF = spark.curatedFilesDF(sourceSystem)
    curatedFilesDF.show(false)

    val loadedDF = loadFiles(spark, curatedFilesDF)
    loadedDF.write
      .format("bigquery")
      .mode(SaveMode.Append)
      .option("writeMethod", "direct")
      .option("dataset", INGESTION_INFO_DATASET)
      .option("table", WAREHOUSE_DATASETS_TABLE)
      .save()
  }

  private def loadFiles(
      spark: SparkSession,
      curatedFilesDF: DataFrame
  ): DataFrame = {
    // Running on driver, once need spark session to load parquet.
    val filesToLoad = curatedFilesDF.collect()

    log.info(s"Files to be loaded: ${filesToLoad.length}")

    val rows = filesToLoad
      .map(row => {
        val parquetFile = row.getAs[String]("parquet_file_uri")
        var mappedRow: Row = null

        try {
          mappedRow = loadFile(spark, row)
        } catch {
          case e: Exception =>
            val message =
              e.getMessage + " -- " + ExceptionUtils.getStackTrace(e)
            e.printStackTrace()
            mappedRow = Row(
              parquetFile,
              row.getAs[String]("source"),
              false,
              null,
              null,
              null,
              null,
              message
            )
        }

        mappedRow
      })
      .seq
      .asJava

    spark.createDataFrame(rows, WAREHOUSE_DATASETS_SCHEMA)
  }

  private def loadFile(spark: SparkSession, row: Row): Row = {
    val parquetFile = row.getAs[String]("parquet_file_uri")
    val sourceSystem = row.getAs[String]("source")

    log.info(s"Loading $parquetFile")

    val tableInfo =
      SUS_SYSTEMS_TABLES(sourceSystem)

    val targetTable: (String, String) = if (sourceSystem == "SIA" || sourceSystem == "SUS") {
      tableInfo(getFileType(parquetFile))
    } else
      throw new IllegalArgumentException(
        s"Source system $sourceSystem not implemented."
      )

    val parquetDF = spark.read
      .parquet(parquetFile)
    val entriesCount = parquetDF.count()

    parquetDF.write
      .format("bigquery")
      .mode(SaveMode.Append)
      .option("writeMethod", "direct")
      .option("dataset", targetTable._1)
      .option("table", targetTable._2)
      .save()

    log.info(s"Loaded to $entriesCount to bigquery from $parquetFile")

    Row(
      parquetFile,
      sourceSystem,
      true,
      targetTable._1,
      targetTable._2,
      entriesCount,
      new Date(System.currentTimeMillis()),
      null
    )
  }

  private def getFileType(fileUri: String): String = {
    val lastPart = fileUri.lastIndexOf("/") + 1
    fileUri.substring(lastPart, lastPart + 2)
  }

  implicit class SparkSessionOps(spark: SparkSession) {

    def serializedConf() =
      new SerializableConfiguration(spark.sparkContext.hadoopConfiguration)

    def configureForJob(): SparkSession = {
      spark.conf.set("viewsEnabled", true)
      spark.conf.set("spark.sql.parquet.enableVectorizedReader", "false")
      spark.conf.set("materializationDataset", INGESTION_INFO_DATASET)
      spark
    }

    def curatedFilesDF(sourceSystem: String): DataFrame = {
      val sql =
        s"""
            WITH loaded_parquet AS(
              SELECT
                parquet_file_uri
              FROM `$INGESTION_INFO_DATASET.warehouse_datasets`
              WHERE
                success IS TRUE
            )
            SELECT DISTINCT
              c.parquet_file_uri
              , c.source
            FROM `$INGESTION_INFO_DATASET.curated_files` c
            LEFT JOIN `$INGESTION_INFO_DATASET.warehouse_datasets` d
            ON c.parquet_file_uri = d.parquet_file_uri
            WHERE
            c.source = '$sourceSystem'
            AND
            c.success = TRUE
            AND
            (d.parquet_file_uri IS NULL OR d.success IS FALSE)
            AND
            c.parquet_file_uri NOT IN(
              select * from loaded_parquet
            )
          """

      spark.read
        .format("bigquery")
        .load(sql)
    }
  }
}
