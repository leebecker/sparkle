package org.sparkle.typesystem.ops

import epic.slab.{PartOfSpeech, Token}
import org.sparkle.slate._

/**
  * Created by leebecker on 2/5/16.
  */
object EpicPartOfSpeechOps extends PartOfSpeechOps[Token, PartOfSpeech]{
  override def getPosTag[In <: PartOfSpeech](slab: StringSlate, span: Span, partOfSpeech: PartOfSpeech): Option[String] = {
    val res = if (partOfSpeech.tag == null) None else Option(partOfSpeech.tag)
    res
  }

  override def addPosTags[In <: Token](slab: StringSlate, posTags: TraversableOnce[(Span, PartOfSpeech)]):
      StringSlate = slab.addLayer[PartOfSpeech](posTags)

  override def createPosTag(tag: String, token: Token): PartOfSpeech = PartOfSpeech(tag)
}
