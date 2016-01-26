package org.sparkle.spark

import org.scalatest._
import org.sparkle.clearnlp._
import org.sparkle.slab.{Slab, StringSlab}
import org.sparkle.typesystem.basic.{PartOfSpeech, Token, Sentence, Span}
import org.sparkle.typesystem.syntax.dependency._

import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

object SparkTestUtils {

  def pipeline(s: StringSlab[Any]) = (SentenceSegmenterAndTokenizer andThen PosTagger andThen MpAnalyzer).apply(s)

}

class SparkTest extends FunSuite with Matchers {
  val appName = "SparkleSparkTest"
  val conf = new SparkConf().setAppName(appName).setMaster("local[4]")
  conf.set("spark.app.id", appName)
  conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  val sc = new SparkContext(conf)

  // Create a HiveContext instead of a vanilla SQLContext so that
  // we can use Hive UDFs and UDAFs
  val sqlContext = new HiveContext(sc)
  import sqlContext.implicits._

  test("TokenCountsWithSpark") {
    val slabs = Seq(
      Slab("""Words are fun to count.  Counting words is what we do."""),
      Slab("""Whether it be one word, two words or more, we our word count will score."""),
      Slab("""Wording this last set of words to count is wordy.""")
    )

    val mySlab0 = Slab("""Words are fun to count.  Counting words is what we do.""")
    val mySlab1 = SentenceSegmenterAndTokenizer(mySlab0)
    val mySlab2 = PosTagger(mySlab1)
    val mySlab3 = MpAnalyzer(mySlab2)

    val slabRdd = sc.parallelize(slabs).map(SparkTestUtils.pipeline)
    val tokensRdd = slabRdd.map { slab => slab.iterator[Token].toSeq}.flatMap(tokens => tokens)

    val wordCountsRdd = tokensRdd.map{ case (_, token) => (token.token, 1) }.reduceByKey(_ + _)
    val wordCounts = wordCountsRdd.collectAsMap()
    val wordCountsExpected = Map("count" -> 3, "is" -> 2, "," -> 2, "one" -> 1, "of" -> 1, "set" -> 1, "do" -> 1, "or" -> 1, "last" -> 1, "our" -> 1, "wordy" -> 1, "Counting" -> 1, "will" -> 1, "be" -> 1, "words" -> 3, "to" -> 2, "." -> 4, "more" -> 1, "Whether" -> 1, "Wording" -> 1, "are" -> 1, "we" -> 2, "word" -> 2, "it" -> 1, "fun" -> 1, "Words" -> 1, "two" -> 1, "what" -> 1, "this" -> 1, "score" -> 1)
    wordCounts should contain theSameElementsAs wordCountsExpected

    val lemmaCountsRdd = tokensRdd.map{ case (_, token) => (token.lemma.get, 1) }.reduceByKey(_ + _)
    val lemmaCounts = lemmaCountsRdd.collectAsMap()
    lemmaCounts should contain theSameElementsAs Map("#crd#" -> 2, "count" -> 4, "whether" -> 1, "," -> 2, "of" -> 1, "do" -> 1, "set" -> 1, "or" -> 1, "last" -> 1, "wordy" -> 1, "our" -> 1, "will" -> 1, "be" -> 4, "to" -> 2, "." -> 4, "more" -> 1, "we" -> 2, "word" -> 7, "it" -> 1, "fun" -> 1, "what" -> 1, "this" -> 1, "score" -> 1)

    val posCountsRdd = tokensRdd.map{ case (_, token) => (token.pos.get, 1) }.reduceByKey(_ + _)
    val posCounts = posCountsRdd.collectAsMap()
    posCounts should contain theSameElementsAs Map("PRP" -> 3, "NNS" -> 4, "PRP$" -> 1, "," -> 2, "IN" -> 2, "JJ" -> 3, "JJR" -> 1, "NN" -> 4, "VB" -> 4, "WP" -> 1, "." -> 4, "VBZ" -> 2, "CD" -> 2, "TO" -> 2, "DT" -> 1, "VBG" -> 2, "MD" -> 1, "VBP" -> 2, "CC" -> 1)

    val nnCounts = tokensRdd.filter{ case (_, token) => token.pos.getOrElse("").startsWith("NN") }.count()

    val nnCountsExpected = posCounts.getOrElse("NN", 0) + posCounts.getOrElse("NNS", 0) + posCounts.getOrElse("NNP", 0)
    assert(nnCounts == nnCountsExpected)
  }
}
