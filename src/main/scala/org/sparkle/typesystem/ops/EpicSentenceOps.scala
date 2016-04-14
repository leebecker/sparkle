package org.sparkle.typesystem.ops

import org.sparkle.slate._
import epic.slab.Sentence

/**
  * Created by leebecker on 2/5/16.
  */
object EpicSentenceOps extends SentenceOps[epic.slab.Sentence] {
  override def createSentence(): Sentence = Sentence()

  override def selectAllSentences[In <: Sentence](slab: StringSlate): TraversableOnce[(Span, Sentence)] =
    slab.iterator[Sentence]

  override def selectSentences[In <: Sentence](slab: StringSlate, coveringSpan: Span): TraversableOnce[(Span, Sentence)] =
    slab.covered[Sentence](coveringSpan)

  override def addSentences(slab: StringSlate, sentences: TraversableOnce[(Span, Sentence)]):
      StringSlate = slab.addLayer[Sentence](sentences)
}
