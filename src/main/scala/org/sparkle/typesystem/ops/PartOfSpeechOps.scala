package org.sparkle.typesystem.ops

import epic.slab.StringSlab
import epic.trees.Span
import org.sparkle.typesystem.basic.Token

/**
  * Created by leebecker on 2/5/16.
  */
trait PartOfSpeechOps[TOKEN_TYPE, POS_TYPE] {

  def getPosTag[In <: POS_TYPE](slab: StringSlab[In], span: Span, partOfSpeech:POS_TYPE): Option[String]

  def createPosTag(tag: String, token: TOKEN_TYPE): POS_TYPE

  def addPosTags[In <: TOKEN_TYPE](slab: StringSlab[In], posTags: TraversableOnce[(Span, POS_TYPE)]) : StringSlab[In with POS_TYPE]
}
