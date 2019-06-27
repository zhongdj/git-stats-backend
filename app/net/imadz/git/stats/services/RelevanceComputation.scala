package net.imadz.git.stats.services

trait Normalizer[T] {
  def normalize: T => Int

  def denormalize: Int => T
}

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

  def categorize[T](implicit N: Normalizer[T]): List[List[T]] => Set[Set[T]] = xss =>
    classify(count(normalize(xss)))

  private def count(normalized: List[List[Int]]): SymmetryMatrix = ???

  private def classify[T](matrix: SymmetryMatrix)(implicit N: Normalizer[T]): Set[Set[T]] = ???

  private def normalize[T](xss: List[List[T]])(implicit N: Normalizer[T]): List[List[Int]] = {
    xss.map(_.map(N.normalize))
  }
}
