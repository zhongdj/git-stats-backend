package net.imadz.git.stats.workers

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ Actor, ActorRef, OneForOneStrategy, PoisonPill, Props, ReceiveTimeout, SupervisorStrategy, Terminated }
import net.imadz.git.stats.services._
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster.{ Done, Progress, Update }
import net.imadz.git.stats.{ AppError, MD5 }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.ws.WSClient
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class GitRepositoryUpdateJobMaster(taskId: Int, req: CreateTaskReq,
    clone: CloneRepositoryService,
    stat: InsertionStatsService,
    funcStats: FunctionStatsService,
    taggedCommit: TaggedCommitStatsService,
    graphRepository: GraphRepository,
    ws: WSClient,
    protected val dbConfigProvider: DatabaseConfigProvider,
    observer: Option[ActorRef])(implicit ec: ExecutionContext) extends Actor with HasDatabaseConfigProvider[JdbcProfile] with MD5 {

  override def receive: Receive = idle

  var idleRounds = 0

  private var workers: Set[ActorRef] = createWorkers

  private def createWorkers = {
    req.repositories.map(createWorker).toSet
  }

  private def createWorker: GitRepository => ActorRef = r => {
    val worker = context.actorOf(GitRepositoryUpdateJobWorker.props(taskId, r, clone, stat, funcStats, taggedCommit, graphRepository, ws, dbConfigProvider, req.fromDay, req.toDay.get), workerName(r))
    context.watch(worker)
    worker
  }

  private def workerName(r: GitRepository) = {
    s"$taskId-${r.repositoryUrl}-${r.branch}".replaceAll("""/""", "@")
  }

  def idle: Receive = {
    case Update =>
      context.become(updating)
      workers.foreach(_ ! Update)
    case ReceiveTimeout =>
      workers = createWorkers
      self ! Update
  }

  def updating: Receive = {
    case d @ Done(repo, message) =>
      println(message)
      context.stop(sender())
      observer.foreach(_ ! d)
    case p: Progress =>
      observer.foreach(_ ! p)
    case Update =>
      context.setReceiveTimeout(1 minute)
    case ReceiveTimeout =>
      context.become(idle)
      context.setReceiveTimeout(Duration.Undefined)
      self ! Update
    case Terminated(worker) =>
      println(worker.path)
      workers -= worker
      if (workers.isEmpty) {
        context.become(idle)
        context.setReceiveTimeout(240 minutes)
      }
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case t: Throwable =>
      println(s"came across error with: ${t.getMessage}")
      t.printStackTrace()
      Restart
  }
}

object GitRepositoryUpdateJobMaster {
  def props(taskId: Int, createTaskReq: CreateTaskReq,
    clone: CloneRepositoryService, stat: InsertionStatsService, funcStats: FunctionStatsService, taggedCommit: TaggedCommitStatsService, graphRepository: GraphRepository, ws: WSClient, dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobMaster(taskId, createTaskReq, clone, stat, funcStats, taggedCommit, graphRepository, ws, dbConfigProvider, None))

  def props(taskId: Int, createTaskReq: CreateTaskReq,
    clone: CloneRepositoryService, stat: InsertionStatsService, funcStats: FunctionStatsService, taggedCommit: TaggedCommitStatsService, graphRepository: GraphRepository, ws: WSClient, dbConfigProvider: DatabaseConfigProvider, observer: ActorRef)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobMaster(taskId, createTaskReq, clone, stat, funcStats, taggedCommit, graphRepository, ws, dbConfigProvider, Some(observer)))

  trait Cmd

  object Update extends Cmd

  case class Progress(message: String) extends Cmd

  case class Done(repo: GitRepository, result: Either[AppError, String]) extends Cmd

}

