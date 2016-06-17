package org.sparkle.slate.spark

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{BooleanParam, Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql.{DataFrame, Row}
import org.sparkle.slate._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag


object MapPartitionsHelper {
  def wrapPipelineFunc(rdd: RDD[String], func: (StringSlate) => (StringSlate)) = rdd.mapPartitionsWithIndex((idx, iterator) => {
    println(s"running on partition idx $idx")
    iterator.map(Slate(_)).map(func)
  })
}


/**
  *  A [[org.apache.spark.sql.DataFrame DataFrame]] [[org.apache.spark.ml.Transformer transformer]]
  *  which runs a [[org.sparkle.slate.Slate String Slate]] pipeline function
  *  on a text column and subsequently extracts values using a collection of [[org.sparkle.slate.spark.StringSlateExtractor StringSlateExtractors]].
  *
  *  This is useful for running NLP pipelines consisting of [[org.sparkle.slate.AnalysisFunction Slate AnalysisFunctions]] to annotate text
  *  and to extract features for training machine learning algorithms or other analyses.
 *
  * @tparam T1 Type returned by extractors.  The type must be serializable and compatible with Spark DataFrame [[org.apache.spark.sql.types.DataType DataTypes]].  Extracted values
  *            are homogeneous. For heterogeneous extraction use [[org.sparkle.slate.spark.SlateExtractorTransformer2 SlateExtractorTransformer2]] or
  *            [[org.sparkle.slate.spark.SlateExtractorTransformer3 SlateExtractorTransformer3]].
  */
class SlateExtractorTransformer[T1:TypeTag:ClassTag](override val uid: String) extends Transformer {
  def this() = this(Identifiable.randomUID("sparkler_slate_extractor"))

  val slateIdentityFunction = (slate: StringSlate) => slate

  val slatePipelineFunc: Param[(StringSlate)=>(StringSlate)] = new Param(this, "slatePipelineFunc",
    """A function which accepts a slate structure and returns a new one.
      |Most often this will wrap up a StringAnalysisFunction, but it needn't be.
      |By default this is set to an identity function""".stripMargin)

  def getSlatePipelineFunc: (StringSlate)=>(StringSlate) = $(slatePipelineFunc)

  def setSlatePipelineFunc(value: (StringSlate)=>(StringSlate)): this.type = set(slatePipelineFunc, value)
  setDefault(slatePipelineFunc->slateIdentityFunction)

  val textCol: Param[String] = new Param(this, "textCol", "name of column containing text to be analyzed")

  def getTextCol: String = $(textCol)

  def setTextCol(value: String): this.type = set(textCol, value)
  setDefault(textCol, "text")

  //type ExtractorsType1 = (String, StringSlateExtractor[T1])
  type ExtractorsType1 = StringSlateExtractor[T1]

  val extractors1: Param[Map[String, ExtractorsType1]] = new Param(this, "extractors1",
    "Map of outputColumns and their StringSlateExtractor function objects")

  def getExtractors1: Map[String, ExtractorsType1] = $(extractors1)

  def setExtractors1(value: Map[String, ExtractorsType1]): this.type = set("extractors1", value)
  setDefault(extractors1 -> Map())

  val mapPartitionsOnPipeline: BooleanParam = new BooleanParam(this, "mapPartitionsOnPipeline",
    """
      |Turning this on will run the pipeline function separately on each partition instead of
      |on each element.  Useful when pipeline has large initialization cost.
    """.stripMargin)

  def getMapPartitionsOnPipeline: Boolean = $(mapPartitionsOnPipeline)

  def setMapPartitionsOnPipeline(value: Boolean): this.type = set("mapPartitionsOnPipeline", value)
  setDefault(mapPartitionsOnPipeline -> true)

  val mapPartitionsOnExtractors: BooleanParam = new BooleanParam(this, "mapPartitionsOnExtractors",
    """
      |Turning this on will run the extractor functions separately on each partition instead of
      |on each element.  This is especially useful when extractors have large initialization
      |cost.
    """.stripMargin)

  def getMapPartitionsOnExtractors: Boolean = $(mapPartitionsOnExtractors)

  def setMapPartitionsOnExtractors(value: Boolean): this.type = set("mapPartitionsOnExtractors", value)
  setDefault(mapPartitionsOnExtractors -> false)

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
    val datasetRdd = dataset.rdd
    val textRdd = dataset.select(col($(textCol))).map(_.getAs[String](0))
    val extractedRdd = extractFromText(textRdd)

    // Combine extracted output with original input in a RowRDD
    val datasetWithExtractor1Rdd = datasetRdd.zip(extractedRdd).map{case (rows, newCols) => Row(rows.toSeq ++ newCols: _*)}

    // Convert back to a DataFrame
    dataset.sqlContext.createDataFrame(datasetWithExtractor1Rdd, transformSchema(dataset.schema))
  }

  def getExtractors: Seq[(String, StringSlateExtractor[_])] = getExtractors1.toSeq
  def getExtractorSchemas: Seq[StructField] = {
    val dataType1 = ScalaReflection.schemaFor[T1].dataType
    for ((extractorName, extractor) <- getExtractors1.toSeq)
      yield StructField(extractorName, dataType1)
  }


  def extractFromText(textRdd: RDD[String]): RDD[Seq[_]] = {

    val slateOutRdd = if (getMapPartitionsOnPipeline) {
      MapPartitionsHelper.wrapPipelineFunc(textRdd, getSlatePipelineFunc)
    } else {
      textRdd.map(Slate(_)).map(getSlatePipelineFunc)
    }

    // Run each of the extractors
    val extractedColumnRDDs = if (getMapPartitionsOnExtractors) {
      for ((extractorName, extractor) <- getExtractors) yield slateOutRdd.mapPartitions(iterator => iterator.map(extractor(_)))
    } else {
      for ((extractorName, extractor) <- getExtractors) yield slateOutRdd.map(extractor(_))
    }
    makeZip(extractedColumnRDDs)
  }

  override def copy(extra: ParamMap): org.apache.spark.ml.Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schemaIn: StructType): StructType = StructType(schemaIn ++ getExtractorSchemas)
}

/**
  * Two type version of [[org.sparkle.slate.spark.SlateExtractorTransformer SlateExtractorTransformer]].  Use this instead of the one type version if
  * your extraction types are heterogeneous. For example the extraction flow produces integers, doubles and complex structs.
  *
  * @tparam T1 Type returned by first set of extractors.  The type must be serializable and compatible with Spark DataFrame [[org.apache.spark.sql.types.DataType DataTypes]].
  * @tparam T2 Type returned by second set of extractors. Refer to T1 for type constraints.
  */
class SlateExtractorTransformer2[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag](override val uid: String)
  extends SlateExtractorTransformer[T1] {

  def this() = this(Identifiable.randomUID("sparkler_slate_extractor2"))

  type ExtractorsType2 = StringSlateExtractor[T2]

  val extractors2: Param[Map[String, ExtractorsType2]] = new Param(this, "extractors2",
    "Map of outputColumns and their StringSlateExtractor function objects")

  def getExtractors2: Map[String, ExtractorsType2] = $(extractors2)

  def setExtractors2(value: Map[String, ExtractorsType2]): this.type = set("extractors2", value)
  setDefault(extractors2-> Map())

  override def getExtractors = super.getExtractors ++ getExtractors2.toSeq

  override def getExtractorSchemas = {
    val dataType2 = ScalaReflection.schemaFor[T2].dataType
    val schema2 = for ((extractorName, extractor) <- $(extractors2))
      yield StructField(extractorName, dataType2)
    super.getExtractorSchemas ++ schema2
  }
}

/**
  * Three type version of [[org.sparkle.slate.spark.SlateExtractorTransformer SlateExtractorTransformer]].  Use this instead of the one type version if
  * your extraction types are heterogeneous. For example the extraction flow produces integers, doubles and complex structs.
  *
  * @tparam T1 Type returned by first set of extractors.  The type must be serializable and compatible with Spark DataFrame [[org.apache.spark.sql.types.DataType DataTypes]].
  * @tparam T2 Type returned by second set of extractors. Refer to T1 for type constraints.
  * @tparam T3 Type returned by the third set of extractors.  Refer to T1 for type constraints.
  */
 class SlateExtractorTransformer3[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag, T3:TypeTag:ClassTag](override val uid: String)
  extends SlateExtractorTransformer2[T1,T2] {

  def this() = this(Identifiable.randomUID("sparkler_slate_extractor3"))


  type ExtractorsType3 = StringSlateExtractor[T3]

  val extractors3: Param[Map[String, ExtractorsType3]] = new Param(this, "extractors3",
    "Map of outputColumns and their StringSlateExtractor function objects")

  def getExtractors3: Map[String, ExtractorsType3] = $(extractors3)

  def setExtractors3(value: Map[String, ExtractorsType3]): this.type = set("extractors3", value)
  setDefault(extractors3-> Map())

  override def getExtractors = super.getExtractors ++ getExtractors3

  override def getExtractorSchemas = {
    val dataType = ScalaReflection.schemaFor[T2].dataType
    val schema = for ((extractorName, extractor) <- getExtractors3)
      yield StructField(extractorName, dataType)
    super.getExtractorSchemas ++ schema
  }
}


/**
  * Convenience factories for SlateExtractorTransformer
  */
object SlateExtractorTransformer {
  def apply[T1:TypeTag:ClassTag]() = new SlateExtractorTransformer[T1]()
  def apply[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag]() = new SlateExtractorTransformer2[T1, T2]()
  def apply[T1:TypeTag:ClassTag, T2:TypeTag:ClassTag, T3:TypeTag:ClassTag]() = new SlateExtractorTransformer3[T1, T2, T3]()
}
