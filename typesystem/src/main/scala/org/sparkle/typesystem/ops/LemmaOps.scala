package org.sparkle.typesystem.ops

import org.sparkle.slate._

/**
  * Created by leebecker on 2/5/16.
  */
trait LemmaOps[TOKEN_TYPE, LEMMA_TYPE] {

  def selectLemmas[In <: LEMMA_TYPE](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, LEMMA_TYPE)]

  def getLemma[In <: LEMMA_TYPE](slate: StringSlate, coveringSpan: Span): LEMMA_TYPE

  def getLemmaText[In <: LEMMA_TYPE](slate: StringSlate, span: Span, lemma:LEMMA_TYPE): Option[String]

  def createLemma(lemma: String, token: TOKEN_TYPE): LEMMA_TYPE

  def addLemmas[In <: TOKEN_TYPE](slate: StringSlate, lemmas: TraversableOnce[(Span, LEMMA_TYPE)]) : StringSlate
}
