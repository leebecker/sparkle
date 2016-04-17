package org.sparkle

import org.apache.poi.ss.formula.functions.T
import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Row, UserDefinedFunction}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StructField, StructType}
import org.sparkle.slate._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe._


class SparkleSlateExtractorTransformer[T1:TypeTag:ClassTag](override val uid: String) extends Transformer {
  def this() = this(Identifiable.randomUID("sparkler_slate_extractor"))


  val pipeline: Param[StringAnalysisFunction] = new Param(this, "pipeline", "a pipeline which consumes a Slate structure and produces a Slate structure with resulting analysis")

  def getPipeline: StringAnalysisFunction = $(pipeline)

  def setPipeline(value: StringAnalysisFunction): this.type = set(pipeline, value)

  //@transient var pipeline: StringAnalysisFunction = _
  lazy val pipelineObj = getPipeline

  val textCol: Param[String] = new Param(this, "textCol", "name of column containing text to be analyzed")

  def getTextCol: String = $(textCol)

  def setTextCol(value: String): this.type = set(textCol, value)

  type ExtractorsType1 = Tuple2[String, StringSlateExtractor[T1]]

  val extractors1: Param[Seq[ExtractorsType1]] = new Param(this, "extractors1",
    "Map of outputColumns and their StringSlateExtractor function objects")

  def getExtractors1: Seq[ExtractorsType1] = $(extractors1)

  def setExtractors1(value: Seq[ExtractorsType1]): this.type = set("extractors1", value)

  /*
  lazy val comboUdf1 = generateUdf($(extractors1))

  def generateUdf[T:TypeTag](extractorList: Seq[Tuple2[String, StringSlateExtractor[T]]]) = {
    val f = (text: String) => {
      val slate = Slate(text)
      val x = for ((key, extractor) <- extractorList) yield (key, extractor(slate))
      x
    }
    udf((text: String) => f)
  }
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

  def makeZip(s: Seq[RDD[_]]): RDD[Seq[_]] = {
    if (s.length == 1)
      s.head.map(e => Seq(e))
    else {
      val others = makeZip(s.tail)
      val all = s.head.zip(others)
      all.map(elem => Seq(elem._1) ++ elem._2)
    }
  }

  override def transform(dataset: DataFrame): DataFrame =  {
    val tmpColName = s"{this.uid}_tmp"
    val colNames = $(extractors1).map(_._1)

    import org.apache.spark.sql.catalyst.ScalaReflection
    val datasetRdd = dataset.rdd
    val textRdd = dataset.select(col($(textCol))).map(_.getAs[String](0))
    val slateInRdd = textRdd.map(Slate(_))
    @transient val slateOutRdd = slateInRdd.map(org.sparkle.clearnlp.SentenceSegmenterAndTokenizer(_))

      //pipelineObj(_))
    val schema1 = ScalaReflection.schemaFor[T1].dataType

    val schemaAndColumn = for ((extractorName, extractor) <- $(extractors1))
      yield (StructField(extractorName, schema1),  slateOutRdd.map(slate => extractor(slate)))

    val columns = makeZip(schemaAndColumn.map(_._2))
    val schemas = schemaAndColumn.map(_._1)
    val schema = StructType(dataset.schema ++ schemas)
    val datasetWithExtractor1Rdd = datasetRdd.zip(columns).map{case (rows, newCols) => Row(rows.toSeq ++ newCols: _*)}
    import dataset.sqlContext.implicits._
    dataset.sqlContext.createDataFrame(datasetWithExtractor1Rdd, schema)


    /*
    // Run pipeline and extractors UDF  on text column
    val df = dataset.withColumn(tmpColName, comboUdf1(col($(textCol))))
    // Now pull out data from map structure into separate columns
    extractMapToColumns(df, tmpColName, colNames).drop(tmpColName)
    */
  }

  override def copy(extra: ParamMap): org.apache.spark.ml.Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schema: StructType): StructType = ???
}

//// DAMMIT DAMMIT DAMMIT.. this won't work because it reruns the slate generation.
//class SparkleSlateExtractorTransformer2[T1:TypeTag, T2:TypeTag](override val uid: String) extends SparkleSlateExtractorTransformer[T1] {
//  type ExtractorsType2 = Tuple2[String, StringSlateExtractor[T2]]
//
//  val extractors2: Param[Seq[ExtractorsType1]] = new Param(this, "extractors1",
//    "Map of outputColumns and their StringSlateExtractor function objects")
//
//  def getExtractors2: Seq[ExtractorsType1] = $(extractors2)
//
//  def setExtractors2(value: Seq[ExtractorsType1]): this.type = set("extractors1", value)
//
//  /*
//  lazy val comboUdf2 = generateUdf($(extractors2))
//  override def transform(dataset: DataFrame): DataFrame =  {
//    val tmpColName = s"{this.uid}_tmp2"
//    val colNames = $(extractors2).map(_._1)
//
//    super.transform(dataset)
//    val df = super.transform(dataset).
//      withColumn(tmpColName, comboUdf2(col($(textCol))))
//    // Now pull out data from map structure into separate columns
//    extractMapToColumns(df, tmpColName, colNames).drop(tmpColName)
//
//  }
//  */
//}