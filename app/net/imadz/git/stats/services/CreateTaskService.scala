package net.imadz.git.stats.services

import akka.actor.ActorSystem
import com.google.inject.Inject
import net.imadz.git.stats.MD5
import net.imadz.git.stats.models.Tables._
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster.Update
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

class CreateTaskService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, actorSystem: ActorSystem,
    clone: CloneRepositoryService, stats: InsertionStatsService)(implicit ec: ExecutionContext)
  extends Constants
  with HasDatabaseConfigProvider[JdbcProfile]
  with MD5 {

  import dbConfig.profile.api._

  def exec(repositories: List[GitRepository], startDate: String, endDate: String): Future[CreateTaskResp] = {
    dbConfig.db.run(
      Task.filter(_.fingerPrint === fingerPrintOf(repositories))
        .result
        .headOption
        .map(r => r.map(row => Future.successful(row.id)))
    ).flatMap(_.getOrElse(createTask(repositories, startDate, endDate)))
      .map { id =>
        createMaster(repositories, startDate, endDate, id) ! Update
        id
      }
      .map(CreateTaskResp.apply)
  }

  private def createMaster(repositories: List[GitRepository], startDate: String, endDate: String, id: Int) = {
    actorSystem.actorOf(GitRepositoryUpdateJobMaster.props(id, CreateTaskReq(repositories, startDate, Some(endDate)), clone, stats, dbConfigProvider), id.toString)
  }

  private def fingerPrintOf(repositories: List[GitRepository]) = {
    hash(repositories.map(r => r.repositoryUrl + ", " + r.branch).sorted)
  }

  def createTask(repositories: List[GitRepository], startDate: String, endDate: String): Future[Int] = {
    val query = (Task returning (Task map (_.id))) += TaskRow(0, startDate, endDate, "Draft", "OneTime", fingerPrintOf(repositories), System.currentTimeMillis(), System.currentTimeMillis())
    val eventualId = dbConfig.db.run(query)
    eventualId.flatMap(id => createTaskItems(repositories, id))
      .flatMap(_ => eventualId)
  }

  def createTaskItems(repositories: List[GitRepository], taskId: Int): Future[Unit] = {
    val query = TaskItem ++= repositories.map(r => TaskItemRow(0, taskId, r.repositoryUrl, r.branch, "Created", System.currentTimeMillis(), System.currentTimeMillis()))
    dbConfig.db.run(query)
      .map(_ => ())
  }
}
