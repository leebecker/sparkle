package org.sparkle.opennlp

import epic.slab._
import epic.trees.Span
import org.scalatest.FunSuite
import org.sparkle.typesystem.basic.{Token}

class OpenNlpTest extends FunSuite {

  // =========
  // Analyzers
  // =========
  val stringBegin = (slab: StringSlab[Span]) => slab

  test("OpenNlp sentence segmentation test") {

    val pipeline: StringAnalysisFunction[Any, Sentence] = SentenceSegmenter

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
