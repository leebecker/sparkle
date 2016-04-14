package org.sparkle.typesystem.ops

import org.sparkle.slate._

/**
  * Created by leebecker on 2/5/16.
  */

trait SentenceOps[SENTENCE_TYPE] {
  def createSentence(): SENTENCE_TYPE

  def selectSentences[In <: SENTENCE_TYPE](slab: StringSlate, coveringSpan: Span): TraversableOnce[(Span, SENTENCE_TYPE)]

  def selectAllSentences[In <: SENTENCE_TYPE](slab: StringSlate): TraversableOnce[(Span, SENTENCE_TYPE)]

  def addSentences(slate: StringSlate, sentences: TraversableOnce[(Span, SENTENCE_TYPE)]): StringSlate
}
