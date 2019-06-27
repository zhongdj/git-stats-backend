package net.imadz.git.stats.services

import scala.io.Source

trait Tagged {

  import java.io.File

  type FilePath = String
  type Tag = String
  type Tagger = FilePath => List[Tag]

  def taggerOf(tagFile: String): Tagger = {
    taggerOf(new File(tagFile))
  }

  def taggerOf(file: File): Tagger = {
    Source.fromFile(file).getLines()
      .filter(_.trim.nonEmpty)
      .map(taggers)
      .foldLeft(zero)(compose)
  }

  def compose(f: Tagger, g: Tagger): Tagger = (filePath: FilePath) => f(filePath) ::: g(filePath)

  private val regex = """^(.*)=(.*)$""".r

  private def taggers: String => Tagger = { line => (filePath: FilePath) =>
    line match {
      case regex(key, tags) if filePath.contains(key) => tags.split(",").toList.map(_.trim)
      case _ => Nil
    }
  }

  def zero: FilePath => List[Tag] = _ => Nil
}
