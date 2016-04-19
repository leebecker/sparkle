//package org.sparkle.clearnlp
//
//import java.util.UUID
//
//import edu.emory.clir.clearnlp.component.utils.NLPUtils
//import edu.emory.clir.clearnlp.util.lang.TLanguage
//import org.apache.commons.io.IOUtils
//import org.apache.spark.annotation.DeveloperApi
//import org.apache.spark.ml.Transformer
//import org.apache.spark.ml.param.{Param, ParamMap}
//import org.apache.spark.ml.util.Identifiable
//import org.apache.spark.sql.{DataFrame, Row}
//import org.apache.spark.sql.types.StructType
//import org.apache.spark.sql.types._
//
//
//case class SparkleToken(word: Option[String])
//
//case class SparkleSentence(token: Seq[SparkleToken])
//
//case class SparkleDocument(sentence: Seq[SparkleSentence])
//
///**
//  * Created by leebecker on 3/23/16.
//  */
//class ClearNlpTokenizer(override val uid: String, languageCode: String="ENGLISH") extends Transformer {
//
//  lazy val tokenizer = NLPUtils.getTokenizer(TLanguage.getType(languageCode))
//
//  def this() = this(Identifiable.randomUID("spakrle_clearnlp_tokenizer"))
//
//  val inputCol: Param[String] = new Param(this, "inputCol", "input column name")
//
//  def getInputCol: String = $(inputCol)
//
//  def setInputCol(value: String): this.type = set(inputCol, value)
//
//  val outputCol: Param[String] = new Param(this, "outputCol", "output column name")
//
//  def getOutputCol: String = $(outputCol)
//
//  def setOutputCol(value: String): this.type = set(outputCol, value)
//
//  override def transform(dataset: DataFrame): DataFrame = {
//    val f = {
//      text: String =>
//        val stream  = IOUtils.toInputStream(text)
//        val sentences = tokenizer.segmentize(stream)
//    }
//    dataset.withColumn($(outputCol), dataset("dd"))
//  }
//
//  override def copy(extra: ParamMap): Transformer = ???
//
//  @DeveloperApi
//  override def transformSchema(schema: StructType): StructType = ???
//}
//
///* object ClearNlpTokenizer {
//  private def convertSentenceTokens(sentenceTokens: java.util.List[java.util.List[String]]): Row = {
//
//  }
//
//
//  private def convert(any: Any, dataType: DateType): Any = {
//    (any, dataType) match {
//      case (null, _) => null
//      case (x: java.util.List[_]), ArrayType(elementType, _)) =>
//        x.
//    }
//  }
//
//}
//*/
