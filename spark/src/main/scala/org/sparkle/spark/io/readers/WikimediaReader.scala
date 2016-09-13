package org.sparkle.spark.io.readers

import de.tudarmstadt.ukp.wikipedia.api.WikiConstants.Language
import de.tudarmstadt.ukp.wikipedia.parser.mediawiki.MediaWikiParserFactory
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._


/**
  * Utility schema and methods for reading wikipedia XML into Spark
  */
object WikimediaReader {
  val wikiSchema = StructType(Array(
    StructField("id", LongType, nullable = true),
    StructField("ns", LongType, nullable = true),
    StructField("redirect", StructType(Seq(
      StructField("_VALUE", StringType),
      StructField("_title", StringType))
    ), nullable = true),

    StructField("title", StringType, nullable = true),
    StructField("restrictions", StringType, nullable = true),
    StructField("revision", StructType(Array(
      StructField("comment", StringType, nullable=true),
      StructField("contributor", StructType(Array(
        StructField("id", LongType, nullable=true),
        StructField("username", StringType, nullable=true))
      ), nullable=true),
      StructField("format", StringType, nullable=true),
      StructField("id", LongType, nullable=true),
      StructField("minor", StringType, nullable=true),
      StructField("model", StringType, nullable=true),
      StructField("parentid", LongType, nullable=true),
      StructField("sha1", StringType, nullable=true),
      StructField("text", StringType, nullable=true)
    )), nullable=true)
  ))

  lazy val wikiParser = new MediaWikiParserFactory(Language.english).createParser
  lazy val wikiParserUdf = udf((wikiMarkup: String) => wikiParser.parse(wikiMarkup).getText)

  def read(sparkSession: SparkSession, wikipath: String, parse: Boolean=true): DataFrame = {
    import sparkSession.implicits._
    val xmlDf = sparkSession.read.format("com.databricks.spark.xml").
      schema(wikiSchema).
      options(Map("rowTag" -> "page")).
      load(wikipath).where(isnull($"redirect"))

    if (parse) {
      xmlDf.withColumn("plainText", regexp_replace(wikiParserUdf($"revision.text"), "^\\s+", ""))
    } else {
      xmlDf
    }
  }

}
