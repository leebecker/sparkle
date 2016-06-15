package org.sparkle.nlp4j

import java.io.ObjectInputStream
import javax.xml.parsers.DocumentBuilderFactory

import edu.emory.mathcs.nlp.common.collection.tree.PrefixTree
import edu.emory.mathcs.nlp.common.collection.tuple.Pair
import edu.emory.mathcs.nlp.component.template.feature.Field
import edu.emory.mathcs.nlp.common.util.IOUtils
import edu.emory.mathcs.nlp.component.template.node.NLPNode
import edu.emory.mathcs.nlp.component.template.util.GlobalLexica

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
    val ambiguityClasses = loadLexicon[java.util.Map[String, java.util.List[String]]](lexicaPrefix + "en-ambiguity-classes-simplified-lowercase.xz")
    lexica.setAmbiguityClasses(new Pair(ambiguityClasses, Field.word_form_simplified_lowercase))
    true
  }

  def initWordClusters() = {
    val wordClusters = loadLexicon[java.util.Map[String, java.util.Set[String]]](lexicaPrefix + "en-brown-clusters-simplified-lowercase.xz")
    lexica.setWordClusters(new Pair(wordClusters, Field.word_form_simplified_lowercase))
    wordClusters
  }

  def initNamedEntityGazetteers() = {
    val namedEntityGazetteers = loadLexicon[PrefixTree[String, java.util.Set[String]]](lexicaPrefix + "en-named-entity-gazetteers-simplified.xz")
    lexica.setNamedEntityGazetteers(new Pair(namedEntityGazetteers, Field.word_form_simplified))
    namedEntityGazetteers
  }

  def initWordEmbeddings() = {
    val wordEmbeddings = Some(loadLexicon[java.util.Map[String, Array[Float]]](lexicaPrefix + "en-word-embeddings-undigitalized.xz"))
    lexica.setWordEmbeddings(new Pair(wordEmbeddings.get, Field.word_form_undigitalized))
    wordEmbeddings
  }

  def loadLexicon[T](aLocation: String) = {
    val is = IOUtils.createObjectXZBufferedInputStream(getClass.getResourceAsStream(aLocation)).asInstanceOf[ObjectInputStream]
    is.readObject.asInstanceOf[T]
  }


}
