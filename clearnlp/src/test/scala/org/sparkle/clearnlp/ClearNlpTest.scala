package org.sparkle.clearnlp

import org.scalatest.FunSuite
import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence,Token}
import org.sparkle.typesystem.syntax.dependency._
import org.sparkle.clearnlp

class ClearNlpTest extends FunSuite {

  // =========
  // Analyzers
  // =========
  val stringBegin = (slate: StringSlate) => slate

  test("ClearNLP standalone sentence segmentation test") {

    val pipeline = new SentenceSegmenter()

    val slate = pipeline(Slate( """This is sentence one.  Do you like sentence 2?  Mr. and Dr. Takahashi want to leave!  Go now!"""))
    val sentences = slate.iterator[Sentence].toList
    assert(sentences.map { case (span, _) => slate.spanned(span) } === List(
      """This is sentence one.""",
      """Do you like sentence 2?""",
      """Mr. and Dr. Takahashi want to leave!""",
      """Go now!"""
    ))
  }

  // Tests
  // =========
  test("ClearNLP sentence segmentation and tokenization test") {

    val sentenceSegmenter: StringAnalysisFunction = clearnlp.sentenceSegmenter()
    val tokenizer: StringAnalysisFunction = clearnlp.tokenizer()
    val posTagger: StringAnalysisFunction = clearnlp.posTagger()
    val mpAnalyzer: StringAnalysisFunction = clearnlp.mpAnalyzer()
    val depParser: StringAnalysisFunction = clearnlp.depParser()

    val pipeline = sentenceSegmenter andThen tokenizer andThen posTagger andThen MpAnalyzer andThen DependencyParser
    val slate = pipeline(Slate( """This is sentence one.  Do you like sentence 2?  Mr. and Dr. Takahashi want to leave!  Go now!"""))
    val sentences = slate.iterator[Sentence].toList
    assert(sentences.map{case (span, _) => slate.spanned(span)} === List(
      """This is sentence one.""",
      """Do you like sentence 2?""",
      """Mr. and Dr. Takahashi want to leave!""",
      """Go now!"""
    ))

    val spanAnnotationToText = (spanAnnotationPair: Tuple2[Span, Any]) => spanAnnotationPair match {
      case (span, _) => slate.spanned(span)
    }

    val tokensInSentence0 = slate.covered[Token](sentences.head._1).toList
    assert(tokensInSentence0.map(spanAnnotationToText) === List("This", "is", "sentence", "one", "."))

    val tokensBeforeSentence1 = slate.preceding[Token](sentences(1)._1).toList
    assert(tokensBeforeSentence1.map(spanAnnotationToText) === tokensInSentence0.map(spanAnnotationToText).reverse)

    val tokens = slate.iterator[Token].toList
    val tokensAfterToken18 = slate.following[Token](tokens(18)._1).toList
    val tokensInSentence3 = slate.covered[Token](sentences(3)._1).toList
    assert(tokensAfterToken18.map(spanAnnotationToText) === tokensInSentence3.map(spanAnnotationToText))

    val posTagsInSentence0 = tokensInSentence0.map{ case (span, token) => token.pos.get }
    assert(posTagsInSentence0 === List("DT", "VBZ", "NN", "CD", "."))

    val lemmasInSentence0 = tokensInSentence0.map{ case (span, token) => token.lemma.get }
    assert(lemmasInSentence0 === List("this", "be", "sentence", "#crd#", "."))

    val depRelationsInSentence0 = slate.covered[DependencyRelation](sentences.head._1)
    val triplesInSentence = depRelationsInSentence0.map{case (span, rel) => DependencyUtils.extractTriple(rel)}
    assert(triplesInSentence === List(
      ("attr", "This", "is"),
      ("root", "is", "ROOT"),
      ("attr", "one", "is"),
      ("punct", ".", "is"),
      ("nsubj", "sentence", "one"))
    )

    val depNodesInSentence0 = slate.covered[DependencyNode](sentences.head._1)
    val triplesInSentence0 = depNodesInSentence0.flatMap(nodeSpan => DependencyUtils.extractTriple(nodeSpan._2))

    // Note difference ordering from above.
    assert(triplesInSentence0 === List(
      ("attr", "This", "is"),
      ("root", "is", "ROOT"),
      ("nsubj", "sentence", "one"),
      ("attr", "one", "is"),
      ("punct", ".", "is"))
    )


  }
}
