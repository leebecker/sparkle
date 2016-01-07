package org.sparkle.typesystem.basic

import java.net.URL

import org.sparkle.typesystem.Span

// ===========
// Annotations
// ===========

case class Source(begin: Int, end: Int, url: URL) extends Span
case class Sentence(begin: Int, end: Int, id: Option[String] = None) extends Span
case class Segment(begin: Int, end: Int, id: Option[String] = None) extends Span
case class Token(begin: Int, end: Int, id: Option[String]=None,
  pos: Option[String]=None,
  stem: Option[String]=None,
  lemma: Option[String]=None) extends Span
case class PartOfSpeech(begin: Int, end: Int, tag: String, id: Option[String] = None) extends Span
case class EntityMention(begin: Int, end: Int, entityType: String, id: Option[String] = None) extends Span

