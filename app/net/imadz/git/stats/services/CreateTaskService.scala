package net.imadz.git.stats.services

import akka.actor.{ ActorIdentity, ActorSystem, Identify }
import akka.util.Timeout
import com.google.inject.Inject
import net.imadz.git.stats.{ MD5, models }
import net.imadz.git.stats.models.Tables
import net.imadz.git.stats.models.Tables._
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster.Update
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.ws.WSClient
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class CreateTaskService @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, actorSystem: ActorSystem, ws: WSClient,
    clone: CloneRepositoryService, stats: ProductivityStatsService, funcStats: FunctionStatsService, taggedCommit: TaggedCommitStatsService, graphRepository: GraphRepository)(implicit ec: ExecutionContext)
  extends Constants
  with HasDatabaseConfigProvider[JdbcProfile]
  with MD5 {

  import dbConfig.profile.api._

  def exec(repositories: List[GitRepository], startDate: String, endDate: String): Future[CreateTaskResp] = {
    dbConfig.db.run(
      Task.filter(_.fingerPrint === fingerPrintOf(repositories))
        .result
        .headOption
        .map(r => r.map { row =>
          for {
            items <- findItemByTaskId(row.id)
          } yield (row.id, items)
        })
    ).flatMap(_.getOrElse(createTask(repositories, startDate, endDate)))
      .flatMap {
        case (id: Int, taskItemRows: List[TaskItemRow]) =>
          createMaster(repositories, startDate, endDate, id, taskItemRows)
            .map(_ ! Update)
            .map(_ => id)
      }
      .map(CreateTaskResp.apply)
  }

  private def findItemByTaskId(id: Int): Future[List[TaskItemRow]] =
    dbConfig.db.run(TaskItem.filter(_.taskId === id).result).map(_.toList)

  import akka.pattern.ask

  import scala.concurrent.duration._

  private def createMaster(repositories: List[GitRepository], startDate: String, endDate: String, id: Int, taskItemRows: List[TaskItemRow]) = {
    implicit val duration: Timeout = Timeout(5 seconds)
    (actorSystem.actorSelection("akka://application/user/" + id.toString) ? Identify(1L))
      .mapTo[ActorIdentity]
      .map(identity => identity.getActorRef.get())
      .recover { case _ => instantiateMaster(repositories, startDate, endDate, id, taskItemRows) }
  }

  private def instantiateMaster(repositories: List[GitRepository], startDate: String, endDate: String, id: Int, taskItemRows: List[TaskItemRow]) = {
    val taskItemKeys = taskItemRows.groupBy(_.repositoryUrl).mapValues(_.head.id)
    actorSystem.actorOf(GitRepositoryUpdateJobMaster.props(id, CreateTaskReq(repositories, startDate, Some(endDate)), taskItemKeys, clone, stats, funcStats, taggedCommit, graphRepository, ws, dbConfigProvider), id.toString)
  }

  private def fingerPrintOf(repositories: List[GitRepository]) = {
    hash(repositories.map(r => r.repositoryUrl + ", " + r.branch).sorted)
  }

  def createTask(repositories: List[GitRepository], startDate: String, endDate: String): Future[(Int, List[TaskItemRow])] = {
    val query = (Task returning (Task map (_.id))) += TaskRow(0, startDate, endDate, "Draft", "OneTime", fingerPrintOf(repositories), System.currentTimeMillis(), System.currentTimeMillis())
    val eventualId = dbConfig.db.run(query)
    for {
      taskId <- eventualId
      itemRows <- createTaskItems(repositories, taskId)
    } yield (taskId, itemRows)
  }

  val copyId: (Int, models.Tables.TaskItemRow) => TaskItemRow = (id, row) => row.copy(id = id)

  def createTaskItems(repositories: List[GitRepository], taskId: Int): Future[List[TaskItemRow]] = {
    val rows = repositories.map(r => TaskItemRow(0, taskId, r.repositoryUrl, r.branch, "Created", System.currentTimeMillis(), System.currentTimeMillis()))
    val query = (TaskItem.returning(TaskItem map (_.id))) ++= rows
    dbConfig.db.run(query).map(_.zip(rows).map(copyId.tupled).toList)
  }
}
