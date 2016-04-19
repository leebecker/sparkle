package org.sparkle.slate.extractors

import org.sparkle.slate._
import org.sparkle.slate.spark.StringSlateExtractor
import org.sparkle.typesystem.basic.Token

/**
  * Created by leebecker on 4/18/16.
  */
object TokenCountExtractor extends StringSlateExtractor[Int] {
  override def apply(slate: StringSlate): Int = slate.iterator[Token].size
}
