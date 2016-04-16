package org.sparkle

import epic.slab.Sentence
import org.apache.spark.sql.types._
import org.sparkle.slate.{Span, StringSlate}
import org.sparkle.typesystem.basic.Token




/**
  * Created by leebecker on 4/14/16.
  */
trait StringSlateExtractor[SCHEMA] {
  def apply(slate: StringSlate): SCHEMA
}

//object FooExtractor(flattener: String) extends StringSlateExtractor with Serializable {

object TokenCountExtractor extends StringSlateExtractor {
  override def apply(slate: StringSlate): Int = slate.iterator[Token].size
}

class

object DumbExtractor extends StringSlateExtractor {
  import org.apache.spark.sql.functions._

  override def apply(slate: StringSlate) = Tuple2("y", "string")

}



object NestedExtractor extends StringSlateExtractor {

  case class SentenceSchema(beginIndex: Int, endIndex: Int, tokens: Seq[TokenSchema])

  case class TokenSchema(token: String, pos: String, beginIndex: Int, endIndex: Int)

  case class ResultSchema(text: String, sentences: Seq[SentenceSchema])

  def apply(slate: StringSlate) = {

    val spansAndSentences = slate.iterator[Sentence].toList

    val sentenceStructs = spansAndSentences.map {
      case (sentenceSpan, sentence) => {
        val (spansAndTokens) = slate.covered[Token](sentenceSpan)

        val tokens = spansAndTokens.map {
          case (tokenSpan, token) => TokenSchema(token.token, token.pos.getOrElse(null), tokenSpan.begin, tokenSpan.end)
        }
        SentenceSchema(sentenceSpan.begin, sentenceSpan.end, tokens)
      }
    }
    ResultSchema(text = slate.content, sentences = sentenceStructs)

  }
}


