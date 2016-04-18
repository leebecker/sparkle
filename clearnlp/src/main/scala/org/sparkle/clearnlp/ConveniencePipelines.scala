package org.sparkle.clearnlp

import org.sparkle.slate._
import org.sparkle.typesystem.basic.{Sentence, Token}
import org.sparkle.typesystem.syntax.dependency.{DependencyNode, DependencyRelation}

/**
  * Created by leebecker on 3/31/16.
  */
object ConveniencePipelines extends Serializable {
    lazy val sentenceSegmenterAndTokenizer: StringAnalysisFunction = org.sparkle.clearnlp.SentenceSegmenterAndTokenizer
    lazy val posTagger: StringAnalysisFunction = PosTagger.sparkleTypesPosTagger()
    lazy val mpAnalyzer: StringAnalysisFunction = MpAnalyzer
    lazy val depParser: StringAnalysisFunction = DependencyParser

    lazy val posTaggedPipeline = sentenceSegmenterAndTokenizer andThen posTagger andThen mpAnalyzer
    lazy val parsedPipeline = posTaggedPipeline andThen depParser
}

trait PipelineContainer extends Serializable {
    val pipeline: StringAnalysisFunction
}

object PosTaggerPipeline extends PipelineContainer {
    lazy val sentenceSegmenterAndTokenizer: StringAnalysisFunction = org.sparkle.clearnlp.SentenceSegmenterAndTokenizer
    lazy val posTagger: StringAnalysisFunction = PosTagger.sparkleTypesPosTagger()
    lazy val mpAnalyzer: StringAnalysisFunction = MpAnalyzer
    lazy val pipeline = sentenceSegmenterAndTokenizer andThen posTagger andThen mpAnalyzer
}
