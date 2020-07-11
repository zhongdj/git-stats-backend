package net.imadz.git.stats.services

import net.imadz.git.stats.algo.{ MST, WeightedGraph }

import scala.annotation.tailrec
import scala.io.Source

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
    copy(cooccurrences = cooccurrences.updated(
      x,
      cooccurrences(x).updated(y, cooccurrences(x)(y) + 1)))

}

trait RelevanceComputation {

  def categorize[T](implicit N: Normalizer[T]): List[List[T]] => Set[Set[T]] = xss =>
    classify(xss.size)(count(normalize(xss)))

  def normalize[T](xss: List[List[T]])(implicit N: Normalizer[T]): List[List[Int]] = {
    xss.map(_.map(N.normalize))
  }

  def count(normalized: List[List[Int]]): SymmetryMatrix =
    normalized.foldLeft(SymmetryMatrix()) {
      case (matrix, xs) => countRelevance(matrix, xs)
    }

  @tailrec
  private def countRelevance(matrix: SymmetryMatrix, xs: List[Int]): SymmetryMatrix = xs match {
    case x :: ys => countRelevance(countHead(matrix, x, ys), ys)
    case Nil     => matrix
  }

  private def countHead(matrix: SymmetryMatrix, x: Int, ys: List[Int]) = {
    ys.foldLeft(matrix.increase(x, x))((ms, y) => ms.increase(x, y))
  }

  private def classify[T](total: Int)(matrix: SymmetryMatrix)(implicit N: Normalizer[T]): Set[Set[T]] = {
    val xyz: Map[Int, Map[Int, Double]] = score(total)(matrix)
    val normalized = for {
      (x, relations) <- xyz
      (y, value) <- relations
    } yield (x, y, value)

    normalized.toList.filterNot(tuple3 => tuple3._1 == tuple3._2).sortBy(_._3)
      .map { case (x, y, z) => (N.denormalize(x), N.denormalize(y), z) }
    //.foreach(println)
    val clustered: Set[Map[Int, Map[Int, Double]]] = cluster(xyz)
    Set.empty
  }

  def score(total: Int)(matrix: SymmetryMatrix): Map[Int, Map[Int, Double]] = {
    matrix.cooccurrences.map {
      case (x, xRelations) => x -> xRelations.map {
        case (y, occurrence) => y -> (occurrence.toDouble / total.toDouble)
      }.withDefaultValue(0D)
    }.withDefaultValue(Map.empty.withDefaultValue(0D))
  }

  def cluster(xyz: Map[Int, Map[Int, Double]]): Set[Map[Int, Map[Int, Double]]] = {
    val weighted = for {
      (x, yz) <- xyz
      (y, z) <- yz
      if x != y
    } yield (x, y, z)

    val reversed = weighted.map(t => (t._1, t._2, 1 / t._3))
    val minimalEdges = MST.LazyPrimMST(WeightedGraph(reversed.toList))
      ._2.edges
      .map(edge => (edge.u, edge.v, 1 / edge.weight))

    println(
      minimalEdges.toList.sortBy(_._3).map(triple => links(triple._1, triple._2, triple._3)).mkString(",")
    )
    Set.empty
  }

  def links(from: Int, to: Int, value: Double): String =
    s"""
       |{
       |    source: ${from},
       |    target: ${to},
       |    value: ${value}
       |}
 """.stripMargin
}

object Demo extends App with RelevanceComputation {
  private val xs: List[(String, String)] = Source.fromFile("/Users/barry/Workspaces/external/git-stats-backend/conf/commits.log")
    .getLines()
    .map { line =>
      val t = line.split(",")
      (t(0), t(1))
    }.toList

  private val file2Id: Map[String, Int] = xs.groupBy(_._2).keySet.zipWithIndex.toMap
  private val id2File: Map[Int, String] = file2Id.groupBy(_._2).mapValues(_.head._1)
  implicit val N: Normalizer[String] = FileNormalizer(file2Id, id2File)

  def node(file: String, id: Int, occurences: Int): String =
    s"""
       |{
       |  id : ${id},
       |  name : '${file}',
       |  symbol : 'round',
       |  value : ${occurences},
       |  size : 10
       |}
     """.stripMargin

  val cooccurrence: List[List[String]] = xs.groupBy(_._1).values.toList.map(_.map(_._2))
  val sm = count(normalize(cooccurrence)(N))

  println(file2Id.map(t => node(t._1, t._2, sm.value(t._2, t._2))).mkString(","))

  private case class FileNormalizer(norm: Map[String, Int], denorm: Map[Int, String]) extends Normalizer[String] {
    override def normalize: String => Int = norm

    override def denormalize: Int => String = denorm
  }

  categorize[String].apply(cooccurrence)

}
