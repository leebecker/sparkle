package org.sparkle.typesystem.basic

/**
  * Created by leebecker on 1/7/16.
  */
case class Token(
  token: String,
  pos: Option[String]=None,
  stem: Option[String]=None,
  lemma: Option[String]=None,
  id: Option[String]=None)
