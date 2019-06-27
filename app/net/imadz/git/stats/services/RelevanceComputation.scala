package net.imadz.git.stats.services


case class SymmetryMatrix(cooccurrences: Map[Int, Map[Int, Int]] = Map.empty.withDefaultValue(Map.empty.withDefaultValue(0))) {
  def increase(x: Int, y: Int): SymmetryMatrix =
    if (x >= y) upperLeftIncrease(x, y)
    else upperLeftIncrease(y, x)

  def value(x: Int, y: Int): Int =
    if (x >= y) cooccurrences(x)(y)
    else cooccurrences(y)(x)

  private def upperLeftIncrease(x: Int, y: Int): SymmetryMatrix =
    copy(cooccurrences = cooccurrences.updated(x,
      cooccurrences(x).updated(y, cooccurrences(x)(y) + 1)))

}

trait RelevanceComputation {

  def categorize[T]: List[T] => Set[Set[T]] = ???

}
