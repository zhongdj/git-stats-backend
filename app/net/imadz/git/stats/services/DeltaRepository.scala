package net.imadz.git.stats.services

import java.sql.Date

import com.google.inject.Inject
import net.imadz.git.stats
import net.imadz.git.stats.models.Tables
import net.imadz.git.stats.models.Tables.{ FuncMetric => TFuncMetric, _ }
import net.imadz.git.stats.services.GolangFuncsParser.FuncMetric
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

case class DeltaRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends Traversal with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  def toFunctionMetric: stats.models.Tables.FuncMetricRow => FunctionMetric = row =>
    FunctionMetric(row.taskId, row.taskItemId, row.day, row.projectRoot, row.path, row.name, row.lines, row.complexity, row.complexityPerLine, row.params)

  def listFunctionMetric(taskId: Int, taskItemId: Int): Future[Map[Date, List[FunctionMetric]]] = {
    val q = TFuncMetric.filter(_.taskId === taskId)
      .filter(_.taskItemId === taskItemId)
      .sortBy(_.day.asc)
      .result
    dbConfig.db.run(q)
      .map(_.map(toFunctionMetric).toList.groupBy(_.day))
  }

  def deltaMaxDay(taskId: Int, taskItemId: Int): Future[Option[Date]] = {
    val q = Tables.FuncMetricDelta
      .filter(_.taskId === taskId)
      .filter(_.taskItemId === taskItemId)
      .map(_.day)
      .max

    dbConfig.db.run(q.result)
  }

  def minDate(taskId: Int, taskItemId: Int): Future[Date] = for {
    finishDayOpt <- deltaMaxDay(taskId, taskItemId)
    funcStartDay <- funcMetricDayMin(taskId, taskItemId)
  } yield finishDayOpt.getOrElse(funcStartDay)

  private def funcMetricDayMin(taskId: Int, taskItemId: Int) = {
    val q = dayOfTaskItem(taskId, taskItemId)
      .min
      .result

    dbConfig.db.run(q)
      .map(_.getOrElse(now))
  }

  def maxDate(taskId: Int, taskItemId: Int): Future[Date] = {
    val q = dayOfTaskItem(taskId, taskItemId)
      .max
      .result

    dbConfig.db.run(q)
      .map(_.getOrElse(now))
  }

  private def now = {
    new Date(System.currentTimeMillis())
  }

  private def dayOfTaskItem(taskId: Int, taskItemId: Int) = {
    TFuncMetric
      .sortBy(_.day.asc)
      .filter(_.taskId === taskId)
      .filter(_.taskItemId === taskItemId)
      .map(_.day)
      .distinct
  }

  private def toRow(taskId: Int, taskItemId: Int, projectRoot: String, day: Date): FuncMetric => FuncMetricDeltaRow =
    fm => FuncMetricDeltaRow(0, taskId, taskItemId, day, fm.name, fm.abbrPath, projectRoot, fm.fullPath, fm.lines, fm.params, fm.complexity, fm.complexityRate.toFloat)

  def save(taskId: Int, taskItemId: Int, projectRoot: String, day: Date, metrics: List[FuncMetric]): Future[List[Int]] = {
    val q = FuncMetricDelta.returning(FuncMetricDelta.map(_.id))
    val rows: List[FuncMetricDeltaRow] = metrics.map(toRow(taskId, taskItemId, projectRoot, day))
    dbConfig.db.run(DBIO.sequence(
      rows.map(row => {
        q.insertOrUpdate(row).map(_.getOrElse(0))
      })))
      .map(_.toList)
  }
}
