package org.sparkle.langdetect

import org.scalatest._
import org.sparkle.slate._
import org.sparkle.typesystem.basic.Language

/**
  * Created by leebecker on 11/23/16.
  */
class TestLanguageDetector extends FunSuite {


  val text = """
    |un texte en français
    |a text in english
    |
    |un texto en español
    |
    |un texte un peu plus long en français
    |
    |a text a little longer in english
    |
    | a little longer text in english
    |
    | un texto un poco mas longo en español
    |
    | J'aime les bisounours !
    |
    | Bienvenue à Montmartre !
    |
    | Welcome to London !
    |
    | un piccolo testo in italiano
    |
    | een kleine Nederlandse tekst
    |
    | Matching sur des lexiques
    |
    | Matching on lexicons
    |
    | Une première optimisation consiste à ne tester que les sous-chaînes de taille compatibles avec le lexique.
    |
    | A otimização é a primeira prova de que não sub-canais compatível com o tamanho do léxico.
    |
    |Ensimmäinen optimointi ei pidä testata, että osa-kanavien kanssa koko sanakirja.
    |
    |chocolate
    |
    |some chocolate
    |
    |eating some chocolate
  """.stripMargin

  val expectedLanguages = List("fr", "es", "fr", "en", "en", "es", "fr", "fr", "unknown", "it", "nl", "fr", "en", "fr", "pt", "fi", "it", "it", "en")
  lazy val slate = Slate(text)



  test("Detected languages from text should match expected output") {
    val langdetector = LanguageDetector(segment=true, probThreshold=0.6)
    val actualLanguages = langdetector(slate).indexedSeq[Language].map(l=>l._2.code)
    (actualLanguages zip expectedLanguages).foreach{ case (actual, expected) =>
      assert(actual == expected)
    }
  }

  test("Test Language Window Ops") {
    val langdetector = LanguageDetector(segment=true, probThreshold=0.6)
    val actualLanguages = langdetector(slate).indexedSeq[Language].map(l=>l._2.code)

    val enOps = LanguageWindowOps(Set("en"))
    assert(enOps.selectWindows(langdetector(slate)).size == 4)

    val frOps = LanguageWindowOps(Set("fr"))
    assert(frOps.selectWindows(langdetector(slate)).size == 6)

    val comboOps = LanguageWindowOps(Set("fr", "en"))
    assert(comboOps.selectWindows(langdetector(slate)).size == 10)



  }


}
