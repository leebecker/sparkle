package org.sparkle.spark

import org.apache.spark.annotation.DeveloperApi
import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{BooleanParam, Param, ParamMap}
import org.apache.spark.ml.util.Identifiable
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StructField, StructType}
import org.apache.spark.sql._
import org.sparkle.slate._

import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag


object MapPartitionsHelper {
  def wrapPipelineFunc(rdd: RDD[String], func: (StringSlate) => (StringSlate)) = rdd.mapPartitionsWithIndex((idx, iterator) => {
    println(s"running on partition idx $idx")
    iterator.map(Slate(_)).map(func)
  })
}

case class SlateExtractorTransformerOutput1[T1](extracted1: Map[String, T1])
case class SlateExtractorTransformerOutput2[T1, T2](extracted1: Map[String, T1], extracted2: Map[String, T2])
case class SlateExtractorTransformerOutput3[T1, T2, T3](extracted1: Map[String, T1], extracted2: Map[String, T2], extracted3: Map[String, T3])

/**
  *  A [[org.apache.spark.sql.DataFrame DataFrame]] [[org.apache.spark.ml.Transformer transformer]]
  *  which runs a [[org.sparkle.slate.Slate String Slate]] pipeline function
  *  on a text column and subsequently extracts values using a collection of [[org.sparkle.slate.StringSlateExtractor StringSlateExtractors]].
  *
  *  This is useful for running NLP pipelines consisting of [[org.sparkle.slate.AnalysisFunction Slate AnalysisFunctions]] to annotate text
  *  and to extract features for training machine learning algorithms or other analyses.
 *
  * @tparam T1 Type returned by extractors.  The type must be serializable and compatible with Spark DataFrame [[org.apache.spark.sql.types.DataType DataTypes]].  Extracted values
  *            are homogeneous. For heterogeneous extraction use [[org.sparkle.spark.SlateExtractorTransformer2 SlateExtractorTransformer2]] or
  *            [[org.sparkle.spark.SlateExtractorTransformer3 SlateExtractorTransformer3]].
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

  def extract(sparkSession: SparkSession, slateRdd: RDD[StringSlate]) = {
    import sparkSession.implicits._

    val extractFunc = (slate: StringSlate) => SlateExtractorTransformerOutput1(runExtractors1(slate))

    val extractedRdd = if (getMapPartitionsOnExtractors) {
      slateRdd.mapPartitions(slateIter => slateIter.map(extractFunc))
    } else {
      slateRdd.map(extractFunc)
    }
    extractedRdd.toDS().toDF()
  }


  def flattenExtracted(extractedDf: DataFrame) = {
    $(extractors1).keys.
      foldLeft(extractedDf)((df, colname) => df.withColumn(colname, col(s"extracted1.$colname"))).
      drop("extracted1")
  }

  override def transform(dataset: Dataset[_]): DataFrame =  {

    import dataset.sparkSession.implicits._
    val datasetRowsRdd = dataset.toDF.rdd
    val textRdd = datasetRowsRdd.map(_.getAs[String](getTextCol))

    // Run slate pipeline and extract features
    val slateRdd = runSlatePipelineFunc(textRdd)
    val extractedDf = extract(dataset.sparkSession, slateRdd)

    // Zip RDDs from original dataset and the extracted values
    // This is possible because the operations thus far have not
    // changed the number of partitions nor the number of rows
    val mergedRowsRdd = (datasetRowsRdd zip extractedDf.rdd).map {
      case (rowLeft, rowRight) => Row.fromSeq(rowLeft.toSeq ++ rowRight.toSeq)
    }
    val mergedSchema = StructType(dataset.schema.fields ++ extractedDf.schema.fields)
    val mergedDf = dataset.sparkSession.createDataFrame(mergedRowsRdd, StructType(mergedSchema))

    // Flatten extracted columns into columns of their own
    flattenExtracted(mergedDf)
  }

  def getExtractors: Seq[(String, StringSlateExtractor[_])] = getExtractors1.toSeq
  def getExtractorSchemas: Seq[StructField] = {
    val dataType1 = ScalaReflection.schemaFor[T1].dataType
    for ((extractorName, extractor) <- getExtractors1.toSeq)
      yield StructField(extractorName, dataType1)
  }

  def runExtractors1(slate: StringSlate): Map[String, T1] = {
    getExtractors1.map{ case (extractorName, extractor) => (extractorName, extractor(slate)) }
  }

  def runSlatePipelineFunc(textRdd: RDD[String]) =
    if (getMapPartitionsOnPipeline) {
      MapPartitionsHelper.wrapPipelineFunc(textRdd, getSlatePipelineFunc)
    } else {
      textRdd.map(Slate(_)).map(getSlatePipelineFunc)
    }

  override def copy(extra: ParamMap): org.apache.spark.ml.Transformer = defaultCopy(extra)

  @DeveloperApi
  override def transformSchema(schemaIn: StructType): StructType = StructType(schemaIn ++ getExtractorSchemas)
}

/**
  * Two type version of [[org.sparkle.spark.SlateExtractorTransformer SlateExtractorTransformer]].  Use this instead of the one type version if
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


  def runExtractors2(slate: StringSlate): Map[String, T2] = {
      getExtractors2.map{ case (extractorName, extractor) => (extractorName, extractor(slate)) }
  }

  override def extract(sparkSession: SparkSession, slateRdd: RDD[StringSlate]): DataFrame = {
    import sparkSession.implicits._

    val extractFunc = (slate: StringSlate) =>
       SlateExtractorTransformerOutput2(runExtractors1(slate), runExtractors2(slate))

    val extractedRdd = if (getMapPartitionsOnPipeline) {
      slateRdd.mapPartitions(slateIter => slateIter.map(extractFunc))
    } else {
      slateRdd.map(extractFunc)
    }
    extractedRdd.toDS().toDF()
  }


  override def flattenExtracted(extractedDf: DataFrame): DataFrame = {
    val extract1 = super.flattenExtracted(extractedDf)
    $(extractors2).keys.
      foldLeft(extract1)((df, colname) => df.withColumn(colname, col(s"extracted2.$colname"))).
      drop("extracted2")
  }
}

/**
  * Three type version of [[org.sparkle.spark.SlateExtractorTransformer SlateExtractorTransformer]].  Use this instead of the one type version if
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

  def runExtractors3(slate: StringSlate): Map[String, T3] = {
      getExtractors3.map{ case (extractorName, extractor) => (extractorName, extractor(slate)) }
  }

  override def extract(sparkSession: SparkSession, slateRdd: RDD[StringSlate]): DataFrame = {
    import sparkSession.implicits._

    val extractFunc = (slate: StringSlate) =>
       SlateExtractorTransformerOutput3(runExtractors1(slate), runExtractors2(slate), runExtractors3(slate))

    val extractedRdd = if (getMapPartitionsOnPipeline) {
      slateRdd.mapPartitions(slateIter => slateIter.map(extractFunc))
    } else {
      slateRdd.map(extractFunc)
    }
    extractedRdd.toDS().toDF()
  }

  override def flattenExtracted(extractedDf: DataFrame): DataFrame = {
    val extract12 = super.flattenExtracted(extractedDf)
    $(extractors3).keys.
      foldLeft(extract12)((df, colname) => df.withColumn(colname, col(s"extracted3.$colname"))).
      drop("extracted3")
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

