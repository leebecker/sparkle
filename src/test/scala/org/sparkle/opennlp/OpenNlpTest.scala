package org.sparkle.opennlp

import org.scalatest.FunSuite
import org.sparkle.slab.{Slab, StringSlab}
import org.sparkle.typesystem.basic.{Sentence, Span, Token}

class OpenNlpTest extends FunSuite {

  // =========
  // Analyzers
  // =========
  val stringBegin = (slab: StringSlab[Span]) => slab

  test("OpenNlp sentence segmentation test") {

    val pipeline = SentenceSegmenter

    val slab = pipeline(Slab(
      """

        This is sentence one.
        Do you like sentence two? Mr. and Dr. Takahashi want to leave!  Go now!

      """))

    for ((span, sent) <- slab.iterator[Sentence]) {

    }
    val sentences = slab.iterator[Sentence].toList
    assert(sentences.map { case (span, _) => slab.spanned(span) } === List(
      """This is sentence one.""",
      """Do you like sentence two?""",
      """Mr. and Dr. Takahashi want to leave!""",
      """Go now!"""
    ))
  }

}
