package org.sparkle.typesystem.ops

import epic.slab.{StringSlab, Sentence}
import epic.trees.Span

/**
  * Created by leebecker on 2/5/16.
  */
object SparkleSentenceOps extends SentenceOps[epic.slab.Sentence] {
  override def createSentence(): Sentence = Sentence()

  override def selectAllSentences[In <: Sentence](slab: StringSlab[In]): TraversableOnce[(Span, Sentence)] =
    slab.iterator[Sentence]

  override def selectSentences[In <: Sentence](slab: StringSlab[In], coveringSpan: Span): TraversableOnce[(Span, Sentence)] =
    slab.covered[Sentence](coveringSpan)

  override def addSentences[In](slab: StringSlab[In], sentences: TraversableOnce[(Span, Sentence)]):
      StringSlab[In with Sentence] = slab.addLayer[Sentence](sentences)
}
