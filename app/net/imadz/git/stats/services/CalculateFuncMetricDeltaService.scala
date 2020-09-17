package net.imadz.git.stats.services

import java.sql.Date

import com.google.inject.Inject
import net.imadz.git.stats.services.GolangFuncsParser.FuncMetric
import org.joda.time.{ DateTime, Period }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class CalculateFuncMetricDeltaService @Inject() (deltaRepository: DeltaRepository) {

  private def maxDate(taskId: Int, taskItemId: Int): Future[Date] = deltaRepository.maxDate(taskId, taskItemId)

  private def minDate(taskId: Int, taskItemId: Int): Future[Date] = deltaRepository.minDate(taskId, taskItemId)

  def samplingData(taskId: Int, taskItemId: Int): Future[Map[Date, List[FunctionMetric]]] = deltaRepository.listFunctionMetric(taskId, taskItemId)

  def toFuncMetric(delta: List[FunctionMetric]): List[GolangFuncsParser.FuncMetric] =
    delta.map(d => FuncMetric(d.name, d.path, d.path, d.totalLines, d.params, d.cyclomatic, d.rate))

  def batchSave(deltaMetrics: List[FunctionMetric]): Future[Unit] = Future.sequence {
    deltaMetrics.groupBy(_.day).map {
      case (aDay, metrics) =>
        deltaRepository.save(metrics.head.taskId, metrics.head.taskItemId, metrics.head.projectRoot, aDay, toFuncMetric(metrics))
    }
  }.map(_ => ())

  def exec(taskId: Int, taskItemId: Int): Future[Unit] = for {
    deltaMetrics <- delta(taskId, taskItemId)
    _ <- batchSave(deltaMetrics)
  } yield deltaMetrics.foreach(println)

  private def dateRange(from: DateTime, to: DateTime, step: Period): Iterator[DateTime] = Iterator.iterate(from)(_.plus(step)).takeWhile(!_.isAfter(to)).map(_.withMillisOfDay(0))

  private def dateRange(taskId: Int, taskItemId: Int): Future[Iterator[Date]] = for {
    min <- minDate(taskId, taskItemId)
    max <- maxDate(taskId, taskItemId)
  } yield dateRange(new DateTime(min.getTime), new DateTime(max.getTime), Period.days(1))
    .map(dateTime => new Date(dateTime.toDate.getTime))

  type Aggregation = (List[FunctionMetric], Map[Date, List[FunctionMetric]])

  private def delta(taskId: Int, taskItemId: Int): Future[List[FunctionMetric]] = {
    for {
      days <- dateRange(taskId, taskItemId)
      metricIndex <- samplingData(taskId, taskItemId)
    } yield {
      val accumulator = days.foldLeft[Aggregation]((Nil, Map.empty)) {
        case (acc, day) =>
          calculateDelta(acc, day, metricIndex)
      }
      accumulator._2.values.toList.flatten
    }
  }

  private def calculateDelta(acc: Aggregation, day: Date, metricIndex: Map[Date, List[FunctionMetric]]): Aggregation = {
    if (!metricIndex.contains(day)) acc
    else {
      (metricIndex(day), acc._2 + (day -> diff(metricIndex(day), acc._1, day)))
    }
  }

  private def diff(next: List[FunctionMetric], previous: List[FunctionMetric], aDay: Date): List[FunctionMetric] = {
    MetricLike.delta(previous, next).map(_.copy(day = aDay))
  }
}
