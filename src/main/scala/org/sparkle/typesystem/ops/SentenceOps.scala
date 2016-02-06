package org.sparkle.typesystem.ops

import epic.slab.StringSlab
import epic.trees.Span

/**
  * Created by leebecker on 2/5/16.
  */

trait SentenceOps[SENTENCE_TYPE] {
  def createSentence(): SENTENCE_TYPE

  def selectSentences[In <: SENTENCE_TYPE](slab: StringSlab[In], coveringSpan: Span): TraversableOnce[(Span, SENTENCE_TYPE)]

  def selectAllSentences[In <: SENTENCE_TYPE](slab: StringSlab[In]): TraversableOnce[(Span, SENTENCE_TYPE)]

  def addSentences[In](slab: StringSlab[In], sentences: TraversableOnce[(Span, SENTENCE_TYPE)]):
      StringSlab[In with SENTENCE_TYPE]
}
