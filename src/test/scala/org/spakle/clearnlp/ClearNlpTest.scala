package org.spakle.clearnlp

import epic.slab.{Token, Sentence, Slab, StringSlab}
import epic.trees.Span
import org.scalatest.FunSuite
import org.sparkle.clearnlp.{SentenceSegmenterAndTokenizer}

class ClearNlpTest extends FunSuite {

  // =========
  // Analyzers
  // =========
  val stringBegin = (slab: StringSlab[Span]) => slab

  // =========
  // Tests
  // =========
  test("ClearNLP sentence segmentation and tokenization test") {

    val pipeline = SentenceSegmenterAndTokenizer //andThen PosTagger.posTagger
    val slab = pipeline(Slab( """This is sentence one.  Do you like sentence 2?  Mr. and Dr. Takahashi want to leave!  Go now!"""))
    val sentences = slab.iterator[Sentence].toList
    assert(sentences.map{case (span, _) => slab.spanned(span)} === List(
      """This is sentence one.""",
      """Do you like sentence 2?""",
      """Mr. and Dr. Takahashi want to leave!""",
      """Go now!"""
    ))

    val spanAnnotationToText = (spanAnnotationPair: Tuple2[Span, Any]) => spanAnnotationPair match {
      case (span, _) => slab.spanned(span)
    }

    val tokensInSentence0 = slab.covered[Token](sentences.head._1).toList
    assert(tokensInSentence0.map(spanAnnotationToText) === List("This", "is", "sentence", "one", "."))

    val tokensBeforeSentence1 = slab.preceding[Token](sentences(1)._1).toList
    assert(tokensBeforeSentence1.map(spanAnnotationToText) === tokensInSentence0.map(spanAnnotationToText).reverse)

    val tokens = slab.iterator[Token].toList
    val tokensAfterToken18 = slab.following[Token](tokens(18)._1).toList
    val tokensInSentence3 = slab.covered[Token](sentences(3)._1).toList
    assert(tokensAfterToken18.map(spanAnnotationToText) === tokensInSentence3.map(spanAnnotationToText))
  }
}
