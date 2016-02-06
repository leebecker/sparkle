package org.sparkle.typesystem.ops

import epic.slab.{PartOfSpeech, Token, StringSlab}
import epic.trees.Span

/**
  * Created by leebecker on 2/5/16.
  */
object EpicPartOfSpeechOps extends PartOfSpeechOps[Token, PartOfSpeech]{
  override def getPosTag[In <: PartOfSpeech](slab: StringSlab[In], span: Span, partOfSpeech: PartOfSpeech): Option[String] = {
    val res = if (partOfSpeech.tag == null) None else Option(partOfSpeech.tag)
    res
  }

  override def addPosTags[In <: Token](slab: StringSlab[In], posTags: TraversableOnce[(Span, PartOfSpeech)]):
      StringSlab[In with PartOfSpeech] = slab.addLayer[PartOfSpeech](posTags)

  override def createPosTag(tag: String, token: Token): PartOfSpeech = PartOfSpeech(tag)
}
