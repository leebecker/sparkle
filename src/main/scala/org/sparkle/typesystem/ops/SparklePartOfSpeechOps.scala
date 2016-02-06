package org.sparkle.typesystem.ops

import epic.slab.StringSlab
import epic.trees.Span
import org.sparkle.typesystem.basic.Token

/**
  * Created by leebecker on 2/5/16.
  */
object SparklePartOfSpeechOps extends PartOfSpeechOps[Token, Token]{
  override def getPosTag[In <: Token](slab: StringSlab[In], span: Span, partOfSpeech: Token): Option[String] = partOfSpeech.pos

  override def addPosTags[In <: Token](slab: StringSlab[In], posTags: TraversableOnce[(Span, Token)]):
      StringSlab[In with Token] = slab.removeLayer[Token].addLayer[Token](posTags)

  override def createPosTag(tag: String, token: Token): Token = token.copy(pos=Option(tag))
}
