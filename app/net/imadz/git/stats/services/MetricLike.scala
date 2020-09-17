package net.imadz.git.stats.services

import java.sql.Date

import org.joda.time.{ DateTime, Period, PeriodType }

trait Minusable[T] {
  def -(other: T): Option[T]

  def !(): T
}

trait MetricLike[T] extends Minusable[T] {
  type MetricName
  type MetricValue

  def key: MetricName

  def value: MetricValue
}

case class FunctionMetric(taskId: Int, taskItemId: Int, day: Date, projectRoot: String, path: String, name: String, totalLines: Int, cyclomatic: Int, rate: Float, params: Int) extends MetricLike[FunctionMetric] {
  def fullyQualifiedName = s"$path : $name"

  override type MetricName = String
  override type MetricValue = (Int, Int, Float, Int)

  override def key: MetricName = fullyQualifiedName

  override def value: MetricValue = (totalLines, cyclomatic, rate, params)

  override def -(other: FunctionMetric): Option[FunctionMetric] =
    if (fullyQualifiedName == other.fullyQualifiedName)
      Some(copy(
        totalLines = this.totalLines - other.totalLines,
        cyclomatic = this.cyclomatic - other.cyclomatic,
        rate = this.rate - other.rate,
        params = this.params - other.params)
      )
    else None

  override def !(): FunctionMetric = copy(
    totalLines = -this.totalLines,
    cyclomatic = -this.cyclomatic,
    rate = -this.rate,
    params = -this.params)
}

object MetricLike extends App {
  def delta[S <: MetricLike[S]](xs: List[S], ys: List[S]): List[S] = {
    val xmap: Map[S#MetricName, List[S]] = xs.groupBy(_.key)
    val ymap: Map[S#MetricName, List[S]] = ys.groupBy(_.key)

    //removed
    val removed = xmap.filterKeys(!ymap.contains(_))
    //added
    val added = ymap.filterKeys(!xmap.contains(_))
    //changed
    val changed = ymap.filterKeys(xmap.contains)

    val updated = changed.keys.map(key => ymap(key).head.-(xmap(key).head)).flatMap(_.toList)
    (removed.values.flatten.map(_ ! ()) ++ added.values.flatten ++ updated).toList
  }

}