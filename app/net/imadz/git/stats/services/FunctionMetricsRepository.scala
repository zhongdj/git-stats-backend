package net.imadz.git.stats.services

import java.sql.Date

import com.google.inject.Inject
import net.imadz.git.stats.models.Tables
import net.imadz.git.stats.models.Tables.FuncMetricRow
import net.imadz.git.stats.services.GolangFuncsParser.FuncMetric
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class FunctionMetricsRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends Traversal with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  def toRow(taskId: Int, taskItemId: Int, projectRoot: String, day: Date): FuncMetric => FuncMetricRow = fm => FuncMetricRow(0, taskId, taskItemId, day, fm.name, fm.abbrPath, projectRoot, fm.abbrPath, fm.lines, fm.params, fm.complexity, fm.complexityRate.toFloat)

  def save(taskId: Int, taskItemId: Int, projectRoot: String, now: Date, metrics: List[FuncMetric]): Future[List[Int]] = {
    val q = Tables.FuncMetric.returning(Tables.FuncMetric.map(_.id))
    val rows: List[FuncMetricRow] = metrics.map(toRow(taskId, taskItemId, projectRoot, now))
    dbConfig.db.run(DBIO.sequence(
      rows.map(row => {
        q.insertOrUpdate(row).map(_.getOrElse(0))
      })))
      .map(_.toList)
  }

}