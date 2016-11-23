package org.sparkle.langdetect

import org.sparkle.slate.{Span, StringSlate}
import org.sparkle.typesystem.basic.Language
import org.sparkle.typesystem.ops.WindowOps

/**
  * Created by leebecker on 11/23/16.
  */
class LanguageWindowOps(validLanguages: Set[String]) extends WindowOps[Language] {

  require(validLanguages.forall(l=>l.toLowerCase == l), "Invalid language code: All codes must be lower case.")

  override def selectWindows[In <: Language](slate: StringSlate): TraversableOnce[(Span, Language)] = {
    slate.iterator[Language].filter{case(span, lang)=>validLanguages.contains(lang.code)}
  }
}

object LanguageWindowOps {
  def apply(validLanguages: Set[String]) = new LanguageWindowOps(validLanguages)
}