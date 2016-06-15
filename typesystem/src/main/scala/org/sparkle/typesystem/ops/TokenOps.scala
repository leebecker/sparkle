package org.sparkle.typesystem.ops

import org.sparkle.slate._

/**
  * Created by leebecker on 2/5/16.
  */
trait TokenOps[TOKEN_TYPE] {

  def create(text: String): TOKEN_TYPE

  def selectTokens[In <: TOKEN_TYPE](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, TOKEN_TYPE)]

  def getText[In <: TOKEN_TYPE](slate: StringSlate, tokenSpan: Span, token: TOKEN_TYPE): String

  def selectAllTokens[In <: TOKEN_TYPE](slate: StringSlate): TraversableOnce[(Span, TOKEN_TYPE)]

  def addTokens(slate: StringSlate, tokens: TraversableOnce[(Span, TOKEN_TYPE)]): StringSlate
}
