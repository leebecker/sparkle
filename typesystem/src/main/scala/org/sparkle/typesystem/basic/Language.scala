package org.sparkle.typesystem.basic

/**
  * Created by leebecker on 11/23/16.
  */
case class Language(code: String="unknown", probability: Double= -1.0d) {
  require(code.toLowerCase == code, "Bad code.  All codes should be lower case: "+ code)
}


