package org.sparkle.typesystem.ops

import org.sparkle.slate._
import org.sparkle.typesystem.basic.Token

/**
  * Created by leebecker on 2/5/16.
  */
object SparklePartOfSpeechOps extends PartOfSpeechOps[Token, Token]{
  override def getPosTag[In <: Token](slate: StringSlate, span: Span, partOfSpeech: Token): Option[String] = partOfSpeech.pos

  override def addPosTags[In <: Token](slate: StringSlate, posTags: TraversableOnce[(Span, Token)]): StringSlate =
    slate.removeLayer[Token].addLayer[Token](posTags)

  override def createPosTag(tag: String, token: Token): Token = token.copy(pos=Option(tag))
}
