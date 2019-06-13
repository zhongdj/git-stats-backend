package net.imadz.git.stats.workers

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ Actor, ActorRef, OneForOneStrategy, PoisonPill, Props, ReceiveTimeout, SupervisorStrategy, Terminated }
import net.imadz.git.stats.models.{ Metric, SegmentParser, Tables }
import net.imadz.git.stats.services._
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster.{ Done, Progress, Update }
import net.imadz.git.stats.{ AppError, MD5, services }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.language.postfixOps

class GitRepositoryUpdateJobMaster(taskId: Int, req: CreateTaskReq,
    clone: CloneRepositoryService,
    stat: InsertionStatsService,
    taggedCommit: TaggedCommitStatsService,
    protected val dbConfigProvider: DatabaseConfigProvider,
    observer: Option[ActorRef])(implicit ec: ExecutionContext) extends Actor with HasDatabaseConfigProvider[JdbcProfile] with MD5 {

  override def receive: Receive = idle

  var idleRounds = 0

  private var workers: Set[ActorRef] = req.repositories.map(createWorker).toSet

  private def createWorker: GitRepository => ActorRef = r => {
    val worker = context.actorOf(GitRepositoryUpdateJobWorker.props(taskId, r, clone, stat, taggedCommit, dbConfigProvider, req.fromDay, req.toDay.get), workerName(r))
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
      println(s"being idle for $idleRounds minutes")
      if (idleRounds <= 2) idleRounds += 1
      else context.self ! PoisonPill
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
        context.setReceiveTimeout(5 minute)
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
    clone: CloneRepositoryService, stat: InsertionStatsService, taggedCommit: TaggedCommitStatsService, dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobMaster(taskId, createTaskReq, clone, stat, taggedCommit, dbConfigProvider, None))

  def props(taskId: Int, createTaskReq: CreateTaskReq,
    clone: CloneRepositoryService, stat: InsertionStatsService, taggedCommit: TaggedCommitStatsService, dbConfigProvider: DatabaseConfigProvider, observer: ActorRef)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobMaster(taskId, createTaskReq, clone, stat, taggedCommit, dbConfigProvider, Some(observer)))

  trait Cmd

  object Update extends Cmd

  case class Progress(message: String) extends Cmd

  case class Done(repo: GitRepository, result: Either[AppError, String]) extends Cmd

}

case class GitRepositoryUpdateJobWorker(taskId: Int, repo: services.GitRepository,
    cloneService: CloneRepositoryService,
    statService: InsertionStatsService,
    taggedCommit: TaggedCommitStatsService,
    protected val dbConfigProvider: DatabaseConfigProvider,
    fromDay: String, toDay: String)(implicit ec: ExecutionContext) extends Actor
  with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  val data = TableQuery[Tables.Metric]

  def dateOf(m: Metric): java.sql.Date = {
    println(m.day)
    val time = formatter.parse(m.day).getTime
    new java.sql.Date(time)
  }

  private def toMetricRow(taskId: Long): List[Metric] => List[Tables.MetricRow] = xs =>
    xs.map(x => Tables.MetricRow(0, taskId, dateOf(x), Some(x.project), Some(x.developer), Some(x.metric), Some(x.value)))

  val parent = context.parent

  override def receive: Receive = {
    case Update =>
      cloneService.exec(taskId, repo.repositoryUrl)
        .map { message =>
          parent ! Progress(message)
          analysis.fold(
            e => parent ! Done(repo, Left(e)),
            f => f.foreach(message => parent ! Done(repo, Right(message))))
        }
  }

  import dbConfig.profile.api._

  private def analysis = {
    statService.exec(projectPath(taskId, repo.repositoryUrl), fromDay, toDay)
      .map(s => SegmentParser.parse(s.split("""\n""").toList))
      .map(toMetricRow(taskId))
      .map(rows => {
        dbConfig.db.run(DBIO.sequence(rows.map(Tables.Metric.insertOrUpdate)))
          .map(_ => rows.mkString(","))
          .recover {
            case e =>
              e.printStackTrace()
              rows.mkString(",")
          }
      }).map { futureStr =>
        futureStr.flatMap { str =>
          println(s"tagging: ${projectPath(taskId, repo.repositoryUrl)} commit started: ")
          val eventualStr2 = taggedCommit.exec(projectPath(taskId, repo.repositoryUrl))
            .fold(
              e => Future.successful(s"Failed to tag commit with: ${e.message}"),
              s => s
            )
          eventualStr2.map(str2 => str + "\n" + str2)
        }
      }

  }
}

object GitRepositoryUpdateJobWorker {

  def props(taskId: Int, r: services.GitRepository,
    clone: CloneRepositoryService,
    stat: InsertionStatsService,
    taggedCommit: TaggedCommitStatsService,
    dbConfigProvider: DatabaseConfigProvider,
    fromDay: String, toDay: String)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobWorker(taskId, r, clone, stat, taggedCommit, dbConfigProvider, fromDay, toDay))

}
