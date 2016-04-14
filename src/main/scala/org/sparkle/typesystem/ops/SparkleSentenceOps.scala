package org.sparkle.typesystem.ops

import epic.slab.Sentence
import org.sparkle.slate._

/**
  * Created by leebecker on 2/5/16.
  */
object SparkleSentenceOps extends SentenceOps[Sentence] {
  override def createSentence(): Sentence = Sentence()

  override def selectAllSentences[In <: Sentence](slab: StringSlate): TraversableOnce[(Span, Sentence)] =
    slab.iterator[Sentence]

  override def selectSentences[In <: Sentence](slab: StringSlate, coveringSpan: Span): TraversableOnce[(Span, Sentence)] =
    slab.covered[Sentence](coveringSpan)

  override def addSentences(slate: StringSlate, sentences: TraversableOnce[(Span, Sentence)]): StringSlate = slate.addLayer[Sentence](sentences)
}
