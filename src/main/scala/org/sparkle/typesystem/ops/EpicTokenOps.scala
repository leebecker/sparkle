package org.sparkle.typesystem.ops

import epic.slab.{StringSlab, Token}
import epic.trees.Span

/**
  * Created by leebecker on 2/5/16.
  */
object EpicTokenOps extends TokenOps[Token]{
  override def create(text: String): Token = Token(text)

  override def selectAllTokens[In <: Token](slab: StringSlab[In]): TraversableOnce[(Span, Token)] = slab.iterator[Token]

  override def selectTokens[In <: Token](slab: StringSlab[In], coveringSpan: Span): TraversableOnce[(Span, Token)] =
    slab.covered[Token](coveringSpan)

  override def addTokens[In](slab: StringSlab[In], tokens: TraversableOnce[(Span, Token)]):
      StringSlab[In with Token] = slab.addLayer[Token](tokens)
}
