package org.sparkle.typesystem.ops

import org.sparkle.slate._
import org.sparkle.typesystem.basic.Token

/**
  * Created by leebecker on 2/5/16.
  */
object SparklePartOfSpeechOps extends PartOfSpeechOps[Token, Token]{
  override def selectPosTags[In <: Token](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, Token)] = slate.covered[Token](coveringSpan)

  override def getPos[In <: Token](slate: StringSlate, coveringSpan: Span): Token = slate.covered[Token](coveringSpan).toIndexedSeq(0)._2

  override def getPosTag[In <: Token](slate: StringSlate, span: Span, partOfSpeech: Token): Option[String] = partOfSpeech.pos

  override def addPosTags[In <: Token](slate: StringSlate, posTags: TraversableOnce[(Span, Token)]): StringSlate =
    slate.removeLayer[Token].addLayer[Token](posTags)

  override def createPosTag(tag: String, token: Token): Token = token.copy(pos=Option(tag))

}
