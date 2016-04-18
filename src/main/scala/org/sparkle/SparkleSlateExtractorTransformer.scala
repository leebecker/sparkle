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
import org.apache.spark.sql.catalyst.ScalaReflection
import org.sparkle.clearnlp._


class SparkleSlateExtractorTransformer[T1:TypeTag:ClassTag](override val uid: String) extends Transformer {
  def this() = this(Identifiable.randomUID("sparkler_slate_extractor"))

  val slatePipelineFunc: Param[(StringSlate)=>(StringSlate)] = new Param(this, "slatePipelineFunc",
    "A function which accepts a slate structure and returns a new one. " +
    "Most often this will wrap up a StringAnalysisFunction, but it needn't be.")

  def getSlatePipelineFunc: (StringSlate)=>(StringSlate) = $(slatePipelineFunc)

  def setSlatePipelineFunc(value: (StringSlate)=>(StringSlate)): this.type = set(slatePipelineFunc, value)

  //@transient var pipeline: StringAnalysisFunction = _

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

    val datasetRdd = dataset.rdd
    val textRdd = dataset.select(col($(textCol))).map(_.getAs[String](0))
    val extractedRdd = extractFromText(textRdd)

    /*
    val datasetRdd = dataset.rdd
    val textRdd = dataset.select(col($(textCol))).map(_.getAs[String](0))
    val slateInRdd = textRdd.map(Slate(_))
    val slateOutRdd = slateInRdd.map(org.sparkle.clearnlp.SentenceSegmenterAndTokenizer(_))

    // Run each of the extractors
    val extractedColumnRdds = for ((extractorName, extractor) <- $(extractors1)) yield slateOutRdd.map(extractor(_))
    val extractedRdd = makeZip(extractedColumnRdds)
    */
    // Combine extracted output with original input in a RowRDD
    val datasetWithExtractor1Rdd = datasetRdd.zip(extractedRdd).map{case (rows, newCols) => Row(rows.toSeq ++ newCols: _*)}

    // Convert back to a DataFrame
    import dataset.sqlContext.implicits._
    dataset.sqlContext.createDataFrame(datasetWithExtractor1Rdd, transformSchema(dataset.schema))
  }



  //var pipeline2: StringAnalysisFunction = null
  //def setPipeline2(value: StringAnalysisFunction): Unit = {
    //pipeline2 = value
  //}


  //val f = (slate: StringSlate) => getPipelineContainer.pipeline(slate)

  def extractFromText(textRdd: RDD[String]): RDD[Seq[_]] = {
    val slateInRdd = textRdd.map(Slate(_))
    // Run the pipeline
    //val sentenceSegmenterAndTokenizer: StringAnalysisFunction = SentenceSegmenterAndTokenizer
    //val posTagger: StringAnalysisFunction = PosTagger.sparkleTypesPosTagger()
    //val mpAnalyzer: StringAnalysisFunction = MpAnalyzer

    //val pipeline2 = sentenceSegmenterAndTokenizer andThen posTagger andThen mpAnalyzer
    //val slateOutRdd = slateInRdd.map(ConveniencePipelines.posTaggedPipeline(_))
    val slateOutRdd = slateInRdd.map(getSlatePipelineFunc)

    //val slateOutRdd = slateInRdd.map(task)
    //val slateOutRdd = slateInRdd.map(getPipeline(_))

    // Run each of the extractors
    val extractedColumnRdds = for ((extractorName, extractor) <- $(extractors1)) yield slateOutRdd.map(extractor(_))
    makeZip(extractedColumnRdds)
  }

  override def copy(extra: ParamMap): org.apache.spark.ml.Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schemaIn: StructType): StructType = {
    val dataType1 = ScalaReflection.schemaFor[T1].dataType
    val schemas1 = for ((extractorName, extractor) <- $(extractors1))
      yield StructField(extractorName, dataType1)
    StructType(schemaIn ++ schemas1)
  }
}

