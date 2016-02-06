package org.sparkle.typesystem.ops

import epic.slab.StringSlab
import epic.trees.Span

/**
  * Created by leebecker on 2/5/16.
  */
trait TokenOps[TOKEN_TYPE] {

  def create(text: String): TOKEN_TYPE

  def selectTokens[In <: TOKEN_TYPE](slab: StringSlab[In], coveringSpan: Span): TraversableOnce[(Span, TOKEN_TYPE)]

  def selectAllTokens[In <: TOKEN_TYPE](slab: StringSlab[In]): TraversableOnce[(Span, TOKEN_TYPE)]

  def addTokens[In](slab: StringSlab[In], tokens: TraversableOnce[(Span, TOKEN_TYPE)]): StringSlab[In with TOKEN_TYPE]
}
