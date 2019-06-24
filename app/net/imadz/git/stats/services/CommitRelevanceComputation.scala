package net.imadz.git.stats.services

trait Keyed {
  def id: String

  def key: String
}

case class Classified[T <: Keyed](get: T, degree: Int)

case class Relevance(x: Keyed, y: Keyed, degree: Int)

trait CommitRelevanceComputation {

  def categorize[T <: Keyed]: List[T] => Set[Set[Classified[T]]] = xs =>
    xs.groupBy(_.key).values.toList.map(_.map(Classified(_, 1)).toSet).toSet

}
