package org.sparkle.nlp4j

import java.io.ObjectInputStream
import javax.xml.parsers.DocumentBuilderFactory

import edu.emory.mathcs.nlp.common.collection.tree.PrefixTree
import edu.emory.mathcs.nlp.component.template.feature.Field
import edu.emory.mathcs.nlp.common.util.IOUtils
import edu.emory.mathcs.nlp.component.template.lexicon.{GlobalLexica, GlobalLexicon}
import edu.emory.mathcs.nlp.component.template.node.NLPNode

import collection.JavaConverters._

/**
  * Created by leebecker on 6/14/16.
  */
object Nlp4jUtils {

  val lexicaPrefix = "/edu/emory/mathcs/nlp/lexica/"

  val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
  val xmlDoc = builder.newDocument()
  val root = xmlDoc.createElement("dummy")

  lazy val lexica = new GlobalLexica[NLPNode](root)
  // Needed for POS Tagging
  lazy val ambiguityClasses = initAmbiguityClasses()
  // Needed for POS Tagging and Dependency Parsing
  lazy val wordClusters = initWordClusters()
  // Needed for NER
  lazy val namedEntityGazetteers = initNamedEntityGazetteers()
  lazy val wordEmbeddings = initWordEmbeddings()


  def initAmbiguityClasses() = {
    val ambiguityClasses = loadLexicon[java.util.Map[String, java.util.List[String]]](
      lexicaPrefix + "en-ambiguity-classes-simplified-lowercase.xz",
      Field.word_form_simplified_lowercase,
      "ambiguity_classes"
    )
    if (ambiguityClasses.isDefined) lexica.setAmbiguityClasses(ambiguityClasses.get)
    ambiguityClasses
  }

  def initWordClusters() = {
    val wordClusters = loadLexicon[java.util.Map[String, java.util.Set[String]]](
      lexicaPrefix + "en-brown-clusters-simplified-lowercase.xz",
      Field.word_form_simplified_lowercase,
      "word_clusters")
    if (wordClusters.isDefined) lexica.setWordClusters(wordClusters.get)
    wordClusters
  }

  def initNamedEntityGazetteers() = {
    val namedEntityGazetteers = loadLexicon[PrefixTree[String, java.util.Set[String]]](
      lexicaPrefix + "en-named-entity-gazetteers-simplified.xz",
      Field.word_form_simplified,
      "named_entity_gazetteers"
    )
    if (namedEntityGazetteers.isDefined) lexica.setNamedEntityGazetteers(namedEntityGazetteers.get)
    namedEntityGazetteers
  }

  def initWordEmbeddings() = {
    val wordEmbeddings = loadLexicon[java.util.Map[String, Array[Float]]](
      lexicaPrefix + "en-word-embeddings-undigitalized.xz",
      Field.word_form_undigitalized,
      "word_embeddings"
    )

    if (wordEmbeddings.isDefined) lexica.setWordEmbeddings(wordEmbeddings.get)
    wordEmbeddings
  }

  def loadLexicon[T](aLocation: String, field: Field, name: String) = {
    try {
      val is = IOUtils.createObjectXZBufferedInputStream(getClass.getResourceAsStream(aLocation))
      val lexica = is.readObject.asInstanceOf[T]
      Some(new GlobalLexicon(lexica, field, name))
    } catch {
      case e: Exception => None
    }
  }


}
