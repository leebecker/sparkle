package org.sparkle.typesystem.ops.sparkle

import org.sparkle.slate._
import org.sparkle.typesystem.basic.Sentence
import org.sparkle.typesystem.ops.SentenceOps

/**
  * Created by leebecker on 2/5/16.
  */
object SparkleSentenceOps extends SentenceOps[Sentence] {
  override def createSentence(): Sentence = Sentence()

  override def selectAllSentences[In <: Sentence](slate: StringSlate): TraversableOnce[(Span, Sentence)] =
    slate.iterator[Sentence]

  override def selectSentences[In <: Sentence](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, Sentence)] =
    slate.covered[Sentence](coveringSpan)

  override def addSentences(slate: StringSlate, sentences: TraversableOnce[(Span, Sentence)]): StringSlate = slate.addLayer[Sentence](sentences)
}
