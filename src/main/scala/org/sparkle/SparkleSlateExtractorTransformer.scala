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

    // Combine extracted output with original input in a RowRDD
    val datasetWithExtractor1Rdd = datasetRdd.zip(extractedRdd).map{case (rows, newCols) => Row(rows.toSeq ++ newCols: _*)}

    // Convert back to a DataFrame
    import dataset.sqlContext.implicits._
    dataset.sqlContext.createDataFrame(datasetWithExtractor1Rdd, transformSchema(dataset.schema))
  }

  def getExtractors(): Seq[(String, StringSlateExtractor[_])] = getExtractors1
  def getExtractorSchemas(): Seq[StructField] = {
    val dataType1 = ScalaReflection.schemaFor[T1].dataType
    for ((extractorName, extractor) <- $(extractors1))
      yield StructField(extractorName, dataType1)
  }


  def extractFromText(textRdd: RDD[String]): RDD[Seq[_]] = {
    val slateInRdd = textRdd.map(Slate(_))
    // Run the pipeline
    val slateOutRdd = slateInRdd.map(getSlatePipelineFunc)

    // Run each of the extractors
    val extractedColumnRdds = for ((extractorName, extractor) <- getExtractors) yield slateOutRdd.map(extractor(_))
    makeZip(extractedColumnRdds)
  }

  override def copy(extra: ParamMap): org.apache.spark.ml.Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schemaIn: StructType): StructType = {
    val dataType1 = ScalaReflection.schemaFor[T1].dataType
    val schemas1 = getExtractorSchemas()
    StructType(schemaIn ++ schemas1)
  }
}

class SparkleSlateExtractorTransformer2[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag](override val uid: String)
  extends SparkleSlateExtractorTransformer[T1] {

  def this() = this(Identifiable.randomUID("sparkler_slate_extractor2"))

  type ExtractorsType2 = Tuple2[String, StringSlateExtractor[T2]]

  val extractors2: Param[Seq[ExtractorsType2]] = new Param(this, "extractors2",
    "Map of outputColumns and their StringSlateExtractor function objects")

  def getExtractors2: Seq[ExtractorsType2] = $(extractors2)

  def setExtractors2(value: Seq[ExtractorsType2]): this.type = set("extractors2", value)

  override def getExtractors = super.getExtractors() ++ getExtractors2

  override def getExtractorSchemas = {
    val dataType2 = ScalaReflection.schemaFor[T2].dataType
    val schema2 = for ((extractorName, extractor) <- $(extractors2))
      yield StructField(extractorName, dataType2)
    super.getExtractorSchemas ++ schema2
  }
}

 class SparkleSlateExtractorTransformer3[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag, T3:TypeTag:ClassTag](override val uid: String)
  extends SparkleSlateExtractorTransformer2[T1,T2] {

  def this() = this(Identifiable.randomUID("sparkler_slate_extractor3"))

  type ExtractorsType3 = Tuple2[String, StringSlateExtractor[T3]]

  val extractors3: Param[Seq[ExtractorsType3]] = new Param(this, "extractors3",
    "Map of outputColumns and their StringSlateExtractor function objects")

  def getExtractors3: Seq[ExtractorsType3] = $(extractors3)

  def setExtractors3(value: Seq[ExtractorsType3]): this.type = set("extractors3", value)

  override def getExtractors = super.getExtractors() ++ getExtractors3

  override def getExtractorSchemas = {
    val dataType = ScalaReflection.schemaFor[T2].dataType
    val schema = for ((extractorName, extractor) <- getExtractors3)
      yield StructField(extractorName, dataType)
    super.getExtractorSchemas ++ schema
  }
}




object SparkleSlateExtractorTransformer {
  def create[T1:TypeTag:ClassTag] = new SparkleSlateExtractorTransformer[T1]()
  def create[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag] = new SparkleSlateExtractorTransformer2[T1, T2]()
  def create[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag, T3:TypeTag:ClassTag] = new SparkleSlateExtractorTransformer3[T1, T2, T3]()
}
