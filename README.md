# SparkLE

Spark Language Enginineering (SparkLE) is a Scala-based set of libraries
which aim to make type and annotation-oriented natural language processing
easier by wrapping multiple components in a common type system and by
providing mechanisms for interfacing NLP tools with Apache Spark.

The bulk of Sparkle's functionality centers around manipulation of Slates.  Slates are data structures which represent annotations over spans of texts.  These are were inspired from [UIMA](https://uima.apache.org/)'s CAS data structure and
were heavily repurposed from [Epic's](https://github.com/dlwh/epic) Slab data structure, and were subsequently renamed to `slate` to avoid any compatibility confusion, and the internals were modified to permit serialization and interoperability with Apache Spark RDD's.

Like [ClearTK](https://cleartk.github.io/cleartk/) and NLTK, Sparkle endeavors
to integrate with a wide variety of NLP tools and to offer a diverse range of functionality.

## Prerequisites ##

### Scala ###
 
### Java ###
This code builds on Java 1.8.  Once installed ensure `JAVA_HOME` is set correctly.
```
export JAVA_HOME="$(/usr/libexec/java_home -v 1.8)"
 ```

# Building #

```
sbt compile
```

# Examples #
## Running NLP4J Dependency Parser ##
```
import org.sparkle.slate._
import org.sparkle.nlp4j
import org.sparkle.typesystem.basic._
import org.sparkle.typesystem.syntax.dependency._


// Create NLP pipeline.  For now only English is supported.
val tokenizer = nlp4j.sentenceSegmenterAndTokenizer(addTokens=true)
val posTagger= nlp4j.posTagger()
val lemmatizer = nlp4j.lemmatizer()
val depParser = nlp4j.depParser()

val pipeline = tokenizer andThen posTagger andThen lemmatizer andThen depParser


// Create slate for analysis 
val text = "This is the first sentence.  Sentence two is even less inspired.  Isn't sentence three rather superfluous?"
val slateIn = Slate(text)
val slateOut = pipeline(slateIn)

// Print sentences and tokens in sentences
for ((sentSpan, sentence) <- slateOut.iterator[Sentence]) {
    println( sentSpan, slateOut.spanned(sentSpan))
    for ((tokenSpan, token) <- slateOut.covered[Token](sentSpan)) {
        println(tokenSpan, token.token)
    }
}

// include implicit conversions to make conversion of slate iterators / collections easier

import org.sparkle.slate.utils._

// Print Dependency Graph triples
slateOut.indexedSeq[DependencyNode].annotations.map(DependencyUtils.extractTriple).foreach(println)
```
 

## Using feature transformers ##
```
import org.sparkle.slate._
import org.sparkle.slate.spark.SlateExtractorTransformer
import org.sparkle.slate.extractors._


val docs = sc.parallelize(Seq("This is document one.  It has two sentences.")).toDF("text")
val pi = org.sparkle.nlp4j.standardEnglishPipeline
val pipeline = tokenizer


// Create extractor which returns features of type Int
val extractor = SlateExtractorTransformer[Int]()
val pipelineFunc = (slate: StringSlate) => pipeline(slate)
extractor.setSlatePipelineFunc(pipelineFunc).setTextCol("text")
extractor.setExtractors1(Map(
    "tokenCount" -> TokenCountExtractor,
    "wordCount" -> TokenCountExtractor
))
extractor.transform(docs)
```
