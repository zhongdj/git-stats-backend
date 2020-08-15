package net.imadz.git.stats.services

import com.google.inject.Inject
import net.imadz.git.stats.models
import net.imadz.git.stats.models.Tables
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class GraphRepository @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends Traversal with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  val graphs = TableQuery[models.Tables.Graph]

  def findGraph(taskId: Int, graphName: String): Future[Option[Int]] = {
    dbConfig.db.run(graphs
      .filter(g => g.taskId === taskId)
      .filter(g => g.graphName === graphName).result)
      .map(r => r.headOption.map(_.id))
  }

  def createGraph(taskId: Int, graphName: String, graphId: Int): Future[Int] = {
    val now = System.currentTimeMillis()
    val graph = Tables.GraphRow(0, taskId, graphId, graphName, now, now)
    dbConfig.db
      .run(graphs.returning(graphs.map(_.id)).insertOrUpdate(graph))
      .map(_.head)
  }

}
