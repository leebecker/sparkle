package org.sparkle

import org.apache.poi.ss.formula.functions.T
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.sql.{DataFrame, UserDefinedFunction}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.StructType
import org.sparkle.slate._

import scala.reflect.runtime.universe._


class SparkleSlateExtractorTransformer[T1:TypeTag](override val uid: String) extends Transformer {
  def this() = this(Identifiable.randomUID("sparkler_slate_extractor"))

  //@transient private var pipeline: StringAnalysisFunction = _

  val pipeline: Param[StringAnalysisFunction] = new Param(this, "pipeline", "a pipeline which consumes a Slate structure and produces a Slate structure with resulting analysis")

  def getPipeline: StringAnalysisFunction = $(pipeline)

  def setPipeline(value: StringAnalysisFunction): this.type = set(pipeline, value)

  val textCol: Param[String] = new Param(this, "textCol", "name of column containing text to be analyzed")

  def getTextCol: String = $(textCol)

  def setTextCol(value: String): this.type = set(textCol, value)

  type ExtractorsType1 = Tuple2[String, StringSlateExtractor[T1]]

  val extractors1: Param[Seq[ExtractorsType1]] = new Param(this, "extractors1",
    "Map of outputColumns and their StringSlateExtractor function objects")

  def getExtractors1: Seq[ExtractorsType1] = $(extractors1)

  def setExtractors1(value: Seq[ExtractorsType1]): this.type = set("extractors1", value)


  lazy val comboUdf1 = generateUdf($(extractors1))

    //generateUdf($(extractors))

  def generateUdf[T:TypeTag](extractorList: Seq[Tuple2[String, StringSlateExtractor[T]]]) = {
    val f = (text: String) => {
      val slate = Slate(text)
      val x = for ((key, extractor) <- extractorList) yield (key, extractor(slate))
      x
    }
    udf((text: String) => f)
  }
/*{
      val slate = Slate(text)
      val x = for ((key, extractor) <- extractorList) yield (key, extractor(slate))
      x.toMap[String, T1]
    })
  }
    */


  /*
  @transient val combinedExtractorUdf = udf((text:String) => {
    val slate = Slate(text)
    val x = for ((key, extractor) <- $(extractors)) yield (key, extractor(slate))
    x.toMap
  })
  */

  /* this is not possible because we do not have access to the text until it is in the UDF, so there is no way to propagage
  def runExtractors[T](df: DataFrame, slate: StringSlate, extractorsToRun: Seq[Tuple2[String, StringSlateExtractor[T]]]): DataFrame = {
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
    val colNames = $(extractors1).map(_._1)

    // Run pipeline and extractors UDF  on text column
    val df = dataset.withColumn(tmpColName, comboUdf1(col($(textCol))))
    // Now pull out data from map structure into separate columns
    extractMapToColumns(df, tmpColName, colNames).drop(tmpColName)
  }

  override def copy(extra: ParamMap): org.apache.spark.ml.Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schema: StructType): StructType = ???
}

