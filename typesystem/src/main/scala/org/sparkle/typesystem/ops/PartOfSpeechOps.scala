package org.sparkle.typesystem.ops

import org.sparkle.slate._

/**
  * Created by leebecker on 2/5/16.
  */
trait PartOfSpeechOps[TOKEN_TYPE, POS_TYPE] {

  def selectPosTags[In <: POS_TYPE](slate: StringSlate, coveringSpan: Span): TraversableOnce[(Span, POS_TYPE)]

  def getPos[In <: POS_TYPE](slate: StringSlate, coveringSpan: Span): POS_TYPE

  def getPosTag[In <: POS_TYPE](slate: StringSlate, span: Span, partOfSpeech:POS_TYPE): Option[String]

  def createPosTag(tag: String, token: TOKEN_TYPE): POS_TYPE

  def addPosTags[In <: TOKEN_TYPE](slate: StringSlate, posTags: TraversableOnce[(Span, POS_TYPE)]) : StringSlate
}
