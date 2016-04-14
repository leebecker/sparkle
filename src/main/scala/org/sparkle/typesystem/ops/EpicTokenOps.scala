package org.sparkle.typesystem.ops

import org.sparkle.slate._
import epic.slab.Token

/**
  * Created by leebecker on 2/5/16.
  */
object EpicTokenOps extends TokenOps[Token]{
  override def create(text: String): Token = Token(text)

  override def selectAllTokens[In <: Token](slab: StringSlate): TraversableOnce[(Span, Token)] = slab.iterator[Token]

  override def selectTokens[In <: Token](slab: StringSlate, coveringSpan: Span): TraversableOnce[(Span, Token)] =
    slab.covered[Token](coveringSpan)

  override def addTokens(slab: StringSlate, tokens: TraversableOnce[(Span, Token)]):
      StringSlate = slab.addLayer[Token](tokens)
}
