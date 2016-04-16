package org.sparkle

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StructType}
import org.sparkle.slate._

class SparkleSlateExtractorTransformer(override val uid: String) extends Transformer {
  def this() = this(Identifiable.randomUID("sparkler_slate_extractor"))

  //@transient private var pipeline: StringAnalysisFunction = _

  val pipeline: Param[StringAnalysisFunction] = new Param(this, "pipeline", "a pipeline which consumes a Slate structure and produces a Slate structure with resulting analysis")

  def getPipeline: StringAnalysisFunction = $(pipeline)

  def setPipeline(value: StringAnalysisFunction): this.type = set(pipeline, value)

  val textCol: Param[String] = new Param(this, "textCol", "name of column containing text to be analyzed")

  def getTextCol: String = $(textCol)

  def setTextCol(value: String): this.type = set(textCol, value)

  val extractors: Param[Seq[Tuple2[String, StringSlateExtractor]]] = new Param(this, "extractors",
    "Map of outputColumns and their StringSlateExtractor function objects")

  @transient val combinedExtractorUdf = udf((text:String) => {
    val slate = Slate(text)
    val x = for ((key, extractor) <- $(extractors)) yield (key, extractor(slate))
    x.toMap
  })


  /*
  def runExtractors2(df: DataFrame, slate: StringSlate, extractorsToRun: Seq[Tuple2[String, StringSlateExtractor]]): DataFrame = {
    extractorsToRun match {
      case Nil => df
      case (outputCol, extractor)::remainingExtractors => {
        val func = udf(()=>extractor(slate))
        runExtractors(df.withColumn(outputCol, func()), slate, remainingExtractors)
      }
    }
    df
  }
  */

  def extractMapToColumns(df: DataFrame, mapColName: String, colNames: Seq[String]): DataFrame = colNames match {
    case Nil => df
    case colName::remaining => extractMapToColumns(
      df.withColumn(colName, col(s"$mapColName.$colName")), mapColName, remaining)
    case _ => df
  }

  override def transform(dataset: DataFrame): DataFrame =  {
    val tmpColName = s"{this.uid}_tmp"
    val colNames = $(extractors).map(_._1)

    // Run pipeline and extractors UDF  on text column
    val df = dataset.withColumn(tmpColName, combinedExtractorUdf(col($(textCol))))
    // Now pull out data from map structure into separate columns
    extractMapToColumns(df, tmpColName, colNames).drop(tmpColName)
  }

  override def copy(extra: ParamMap): Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schema: StructType): StructType = ???
}

