package org.sparkle.slate.extractors

import org.sparkle.slate._
import org.sparkle.slate.StringSlateExtractor
import org.sparkle.typesystem.basic.{Sentence, Token}


case class SentenceSchema(beginIndex: Int, endIndex: Int, tokens: Seq[TokenSchema])

case class TokenSchema(token: String, pos: String, beginIndex: Int, endIndex: Int)

case class ResultSchema(text: String, sentences: Seq[SentenceSchema])


/**
  * Created by leebecker on 4/18/16.
  */
object NestedExtractor extends StringSlateExtractor[ResultSchema] with Serializable {

  def apply(slate: StringSlate) = {

    val spansAndSentences = slate.iterator[Sentence].toList

    val sentenceStructs = spansAndSentences.map {
      case (sentenceSpan, sentence) => {
        val (spansAndTokens) = slate.covered[Token](sentenceSpan)

        val tokens = spansAndTokens.map {
          case (tokenSpan, token) => TokenSchema(token.token, token.pos.orNull, tokenSpan.begin, tokenSpan.end)
        }
        SentenceSchema(sentenceSpan.begin, sentenceSpan.end, tokens)
      }
    }
    ResultSchema(text = slate.content, sentences = sentenceStructs)

  }
}
