package org.spakle.clearnlp

import org.scalatest.FunSuite
import org.sparkle.clearnlp.ClearNlp
import org.sparkle.slab.Slab
import org.sparkle.typesystem.Span
import org.sparkle.typesystem.basic.{Sentence, Token}

class ClearNlpTest extends FunSuite {

  // =========
  // Analyzers
  // =========
  import Slab.StringSlab

  val stringBegin = (slab: StringSlab[Span]) => slab

  // =========
  // Tests
  // =========
  test("ClearNLP sentence segmentation and tokenization test") {
    val pipeline = stringBegin andThen ClearNlp.sentenceSegmenterAndTokenizer
    val slab = pipeline(Slab( """This is sentence one.  Do you like sentence 2?  Mr. and Dr. Takahashi want to leave!  Go now!"""))
    val sentences = slab.iterator[Sentence].toList
    assert(sentences.map(_.in(slab).content) === List(
      """This is sentence one.""",
      """Do you like sentence 2?""",
      """Mr. and Dr. Takahashi want to leave!""",
      """Go now!"""
    ))

    val tokensInSentence0 = slab.covered[Token](sentences.head).toList
    assert(tokensInSentence0.map(_.in(slab).content) === List("This", "is", "sentence", "one", "."))

    val tokensBeforeSentence1 = slab.preceding[Token](sentences(1)).toList
    assert(tokensBeforeSentence1.map(_.in(slab).content) === tokensInSentence0.map(_.in(slab).content).reverse)

    val tokens = slab.iterator[Token].toList
    val tokensAfterToken18 = slab.following[Token](tokens(18)).toList
    val tokensInSentence3 = slab.covered[Token](sentences(3)).toList
    assert(tokensAfterToken18.map(_.in(slab).content) === tokensInSentence3.map(_.in(slab).content))
  }
}
