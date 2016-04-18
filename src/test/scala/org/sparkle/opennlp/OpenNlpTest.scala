package org.sparkle.opennlp

import org.sparkle.slate._
import org.scalatest.FunSuite
import org.sparkle.typesystem.basic.{Sentence,Token}

class OpenNlpTest extends FunSuite {

  // =========
  // Analyzers
  // =========
  val stringBegin = (slate: StringSlate) => slate

  test("OpenNlp sentence segmentation test") {

    val pipeline: StringAnalysisFunction = OpenNlpSentenceSegmenter.sentenceSegmenter()

    val slate = pipeline(Slate(
      """

        This is sentence one.
        Do you like sentence two? Mr. and Dr. Takahashi want to leave!  Go now!

      """))

    for ((span, sent) <- slate.iterator[Sentence]) {

    }
    val sentences = slate.iterator[Sentence].toList
    assert(sentences.map { case (span, _) => slate.spanned(span) } === List(
      """This is sentence one.""",
      """Do you like sentence two?""",
      """Mr. and Dr. Takahashi want to leave!""",
      """Go now!"""
    ))
  }

}
