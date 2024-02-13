package br.dev.contrib.gov.sus.opendata.jobs

import org.apache.spark.sql.types.{DataTypes, StructField, StructType}

object Datasets {
  val INGESTION_INFO_DATASET = "ingestion_info"
  val DISCOVERY_TABLE = "discovered_files"
  val CURATED_FILES_TABLE = "curated_files"
  val WAREHOUSE_DATASETS_TABLE = "warehouse_datasets"

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

  val WAREHOUSE_DATASETS_SCHEMA = StructType(
    Seq(
      StructField("parquet_file_uri", DataTypes.StringType, false),
      StructField("source", DataTypes.StringType, false),
      StructField("success", DataTypes.BooleanType, false),
      StructField("dataset", DataTypes.StringType),
      StructField("table", DataTypes.StringType),
      StructField("entries_count", DataTypes.LongType),
      StructField("load_date", DataTypes.DateType),
      StructField("error_message", DataTypes.StringType)
    )
  )

  val SIA_TABLES = Map(
    ("PA", ("informacoes_ambulatoriais", "producao_ambulatorial"))
  )

  val SUS_SYSTEMS_TABLES = Map(
    ("SIA", SIA_TABLES)
  )
}
