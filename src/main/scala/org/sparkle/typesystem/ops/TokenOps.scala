package org.sparkle.typesystem.ops

import org.sparkle.slate._

/**
  * Created by leebecker on 2/5/16.
  */
trait TokenOps[TOKEN_TYPE] {

  def create(text: String): TOKEN_TYPE

  def selectTokens[In <: TOKEN_TYPE](slab: StringSlate, coveringSpan: Span): TraversableOnce[(Span, TOKEN_TYPE)]

  def selectAllTokens[In <: TOKEN_TYPE](slab: StringSlate): TraversableOnce[(Span, TOKEN_TYPE)]

  def addTokens(slab: StringSlate, tokens: TraversableOnce[(Span, TOKEN_TYPE)]): StringSlate
}
