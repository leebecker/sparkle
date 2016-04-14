import epic.slab.Sentence
import org.sparkle.slate.{Slate, Span, StringAnalysisFunction, StringSlate}
import org.sparkle.typesystem.basic.Token

import scala.reflect._


val slate = Slate("This is ok. That is better.")
println(slate.content)


/*
object DumbSentencer extends StringAnalysisFunction {

  def apply(slate: StringSlate) = {
    val sentenceSpans = Seq(Span(0,10), Span(12, 25))
    val sentences = Seq(Sentence(), Sentence())
    slate.addLayer(sentenceSpans.zip(sentences))
  }
}
*/

trait SentenceSegmenter extends StringAnalysisFunction with (String => Iterable[String]) with Serializable {
  override def toString = getClass.getName

  def apply(a: String):IndexedSeq[String] = {
    val slate = Slate(a)
    apply(slate).iterator(classTag[Sentence]).toIndexedSeq.map(s => slate.spanned(s._1))
  }

}

object RegexSentenceSegmenter extends SentenceSegmenter {

  def apply(slate: StringSlate) =
  // the [Sentence] is required because of https://issues.scala-lang.org/browse/SI-7647
    slate.addLayer[Sentence]("[^\\s.!?]+([^.!?]+[.!?]|\\z)".r.findAllMatchIn(slate.content).map(m => Span(m.start, m.end) -> Sentence()))
}

@SerialVersionUID(1)
trait Tokenizer extends StringAnalysisFunction with Serializable with (String=>IndexedSeq[String]) {
  override def toString() = getClass.getName +"()"

  def apply(a: String):IndexedSeq[String] = {
    val slab = apply(Slate(a).append(Span(0, a.length), Sentence()))
    slab.iterator(classTag[Token]).map(_._2.token).toIndexedSeq
  }

}

object RegexTokenizer extends Tokenizer {
  def apply(slab: StringSlate) =
  // the [Token] is required because of https://issues.scala-lang.org/browse/SI-7647
    slab.addLayer(slab.iterator(classTag[Sentence]).flatMap{ case (region, sentence) =>
      "\\p{L}+|\\p{P}+|\\p{N}+".r.findAllMatchIn(slab.content.substring(region.begin, region.end)).map(m =>
        Span(region.begin + m.start, region.begin + m.end) -> Token(m.group(0)))
    })
}

val pipeline = RegexSentenceSegmenter andThen RegexTokenizer
val analysis =  pipeline(slate)
val x = classTag[Token]
println(analysis.iterator(x).toList);


def covering[PARENT:ClassTag,CHILD:ClassTag](slate: StringSlate, parentType: ClassTag[PARENT], childType: ClassTag[CHILD]) = {
  slate.iterator[PARENT](parentType).map{ case (span, parent) =>
      slate.covered[CHILD](span, childType).toList
  }
}

print ("HEREREHERRE")
covering(analysis, classTag[Sentence], classTag[Token])

val tokenClass = "org.sparkle.typesystem.basic.Token"
//println(analysis.iterator(tokenClass).toList);


/*
import scala.reflect.runtime.universe
import scala.reflect.runtime.universe._
import scala.reflect.ManifestFactory

val className = "java.lang.String"
val mirror = universe.runtimeMirror(getClass.getClassLoader)

val cls = Class.forName(tokenClass)
val cls2= ClassTag[Token](cls)
classTag[Token]
analysis.iterator(ClassTag[Token](cls)).toList
typeOf[Token]
typeTag[Token]
*/