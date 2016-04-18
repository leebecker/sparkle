package org.sparkle.typesystem.ops

import org.sparkle.slate._
import org.sparkle.typesystem.basic.Token

/**
  * Created by leebecker on 2/5/16.
  */
object SparkleTokenOps extends TokenOps[Token]{
  override def create(text: String): Token = Token(text)

  override def selectAllTokens[In <: Token](slate: StringSlate): TraversableOnce[(Span, Token)] = slate.iterator[Token]

  override def selectTokens[In <: Token](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, Token)] =
    slate.covered[Token](coveringSpan)

  override def addTokens(slate: StringSlate, tokens: TraversableOnce[(Span, Token)]):
      StringSlate = slate.addLayer[Token](tokens)
}
