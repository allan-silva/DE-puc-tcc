package br.dev.contrib.gov.sus.opendata.jobs

import org.apache.spark.sql.types.{DataType, DataTypes, StructField, StructType}

object Datasets {
  val INGESTION_INFO_DATASET = "ingestion_info"
  val DISCOVERY_TABLE = "discovered_files"
  val CURATED_FILES_TABLE = "curated_files"

  val CURATED_FILES_SCHEMA = StructType(
    Seq(
      StructField("source_file_uri", DataTypes.StringType, false),
      StructField("parquet_file_uri", DataTypes.StringType),
      StructField("source", DataTypes.StringType, false),
      StructField("converted_date", DataTypes.DateType),
      StructField("success", DataTypes.BooleanType, false),
      StructField("error_message", DataTypes.StringType)
    )
  )
}
