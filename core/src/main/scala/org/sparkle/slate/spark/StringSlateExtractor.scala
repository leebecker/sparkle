package org.sparkle.slate.spark

import org.sparkle.slate.StringSlate

/**
  * Created by leebecker on 4/14/16.
  */
trait StringSlateExtractor[SCHEMA] extends Serializable {
  def apply(slate: StringSlate): SCHEMA
}







