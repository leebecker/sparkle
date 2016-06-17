package org.sparkle.typesystem.ops.sparkle

import org.sparkle.slate.{Span, StringSlate}
import org.sparkle.typesystem.basic.Token
import org.sparkle.typesystem.ops.LemmaOps

/**
  * Created by leebecker on 6/15/16.
  */
object SparkleLemmaOps extends LemmaOps[Token, Token]{
  override def selectLemmas[In <: Token](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, Token)] = slate.covered[Token](coveringSpan)

  override def getLemma[In <: Token](slate: StringSlate, coveringSpan: Span): Token = slate.covered[Token](coveringSpan).toIndexedSeq(0)._2

  override def getLemmaText[In <: Token](slate: StringSlate, span: Span, lemma: Token): Option[String] = lemma.lemma

  override def createLemma(lemma: String, token: Token): Token = token.copy(lemma=Option(lemma))

  override def addLemmas[In <: Token](slate: StringSlate, lemmas: TraversableOnce[(Span, Token)]): StringSlate =
    slate.removeLayer[Token].addLayer[Token](lemmas)
}
