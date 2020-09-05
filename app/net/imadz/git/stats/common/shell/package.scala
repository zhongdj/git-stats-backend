package net.imadz.git.stats.common

import scala.sys.process.ProcessBuilder

package object shell {
  def excludeByGrep(excludes: List[String]): ProcessBuilder => ProcessBuilder = pb =>
    if (excludes.isEmpty) pb
    else excludes.map(e => s"grep -v $e").foldLeft(pb) { case (p, c) => p #| c }

  def toMultipleLines: String => List[String] = _.split("""\n""").toList
}
