package org.sparkle.clearnlp

import epic.trees.Span
import org.scalatest.FunSuite
import epic.slab._
import org.sparkle.typesystem.basic.Token
import org.sparkle.typesystem.syntax.dependency._

class ClearNlpTest extends FunSuite {

  // =========
  // Analyzers
  // =========
  val stringBegin = (slab: StringSlab[Span]) => slab

  test("ClearNLP standalone sentence segmentation test") {

    val pipeline = new SentenceSegmenter()

    val slab = pipeline(Slab( """This is sentence one.  Do you like sentence 2?  Mr. and Dr. Takahashi want to leave!  Go now!"""))
    val sentences = slab.iterator[Sentence].toList
    assert(sentences.map { case (span, _) => slab.spanned(span) } === List(
      """This is sentence one.""",
      """Do you like sentence 2?""",
      """Mr. and Dr. Takahashi want to leave!""",
      """Go now!"""
    ))
  }

  // Tests
  // =========
  test("ClearNLP sentence segmentation and tokenization test") {

    val sentenceSegmenter: StringAnalysisFunction[Any, Sentence] = ClearNlpTokenization.sentenceSegmenter()
    val tokenizer: StringAnalysisFunction[Sentence, Token] = ClearNlpTokenization.tokenizer()
    val posTagger: StringAnalysisFunction[Sentence with Token, Token] = PosTagger.sparkleTypesPosTagger()
    val mpAnalyzer: StringAnalysisFunction[Sentence with Token, Token] = MpAnalyzer
    val depParser: StringAnalysisFunction[Sentence with Token, DependencyNode with DependencyRelation] = DependencyParser

    val pipeline = sentenceSegmenter andThen tokenizer andThen posTagger andThen MpAnalyzer andThen DependencyParser
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

    val posTagsInSentence0 = tokensInSentence0.map{ case (span, token) => token.pos.get }
    assert(posTagsInSentence0 === List("DT", "VBZ", "NN", "CD", "."))

    val lemmasInSentence0 = tokensInSentence0.map{ case (span, token) => token.lemma.get }
    assert(lemmasInSentence0 === List("this", "be", "sentence", "#crd#", "."))

    val depRelationsInSentence0 = slab.covered[DependencyRelation](sentences.head._1)
    val triplesInSentence = depRelationsInSentence0.map{case (span, rel) => DependencyUtils.extractTriple(rel)}
    assert(triplesInSentence === List(
      ("attr", "This", "is"),
      ("root", "is", "ROOT"),
      ("attr", "one", "is"),
      ("punct", ".", "is"),
      ("nsubj", "sentence", "one"))
    )

    val depNodesInSentence0 = slab.covered[DependencyNode](sentences.head._1)
    val triplesInSentence0 = depNodesInSentence0.flatMap(x => x match {
      case (s: Span, d: RootDependencyNode) => None
      case (s: Span, d: TokenDependencyNode) => Some(DependencyUtils.extractTriple(d))
    })

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
