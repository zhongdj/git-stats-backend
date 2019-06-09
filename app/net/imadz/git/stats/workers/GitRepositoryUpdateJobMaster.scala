package net.imadz.git.stats.workers

import akka.actor.{Actor, ActorRef, PoisonPill, Props, ReceiveTimeout}
import net.imadz.git.stats.models.{Metric, SegmentParser, Tables}
import net.imadz.git.stats.services._
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster.{Done, Progress, Update}
import net.imadz.git.stats.{AppError, MD5, services}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class GitRepositoryUpdateJobMaster(taskId: Int, req: CreateTaskReq,
                                   clone: CloneRepositoryService,
                                   stat: InsertionStatsService,
                                   protected val dbConfigProvider: DatabaseConfigProvider,
                                   observer: Option[ActorRef])(implicit ec: ExecutionContext) extends Actor with HasDatabaseConfigProvider[JdbcProfile] with MD5 {

  override def receive: Receive = idle

  var busyWorkers = Set.empty[GitRepository]
  var idleRounds = 0

  private lazy val workers: Map[GitRepository, ActorRef] =
    req.repositories.zip(req.repositories
      .map(r => context.actorOf(
        GitRepositoryUpdateJobWorker.props(taskId, r, clone, stat, dbConfigProvider, req.fromDay, req.toDay.get),

        s"$taskId-${r.repositoryUrl}-${r.branch}".replaceAll("""/""", "@"))
      )).toMap

  def idle: Receive = {
    case Update =>
      context.become(updating)
      busyWorkers = req.repositories.toSet
      workers.values.foreach(_ ! Update)
    case ReceiveTimeout =>
      println(s"being idle for $idleRounds minutes")
      if (idleRounds <= 2) idleRounds += 1
      else context.self ! PoisonPill
  }

  def updating: Receive = {
    case d@Done(repo, _) =>
      busyWorkers -= repo
      observer.foreach(_ ! d)
      if (busyWorkers.isEmpty) {
        context.become(idle)
        context.setReceiveTimeout(5 minute)
      }
    case p: Progress =>
      observer.foreach(_ ! p)
    case Update =>
      context.setReceiveTimeout(1 minute)
    case ReceiveTimeout =>
      context.become(idle)
      context.setReceiveTimeout(Duration.Undefined)
      self ! Update

  }
}

object GitRepositoryUpdateJobMaster {
  def props(taskId: Int, createTaskReq: CreateTaskReq,
            clone: CloneRepositoryService, stat: InsertionStatsService, dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobMaster(taskId, createTaskReq, clone, stat, dbConfigProvider, None))

  def props(taskId: Int, createTaskReq: CreateTaskReq,
            clone: CloneRepositoryService, stat: InsertionStatsService, dbConfigProvider: DatabaseConfigProvider, observer: ActorRef)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobMaster(taskId, createTaskReq, clone, stat, dbConfigProvider, Some(observer)))

  trait Cmd

  object Update extends Cmd

  case class Progress(message: String) extends Cmd

  case class Done(repo: GitRepository, result: Either[AppError, String]) extends Cmd

}

case class GitRepositoryUpdateJobWorker(taskId: Int, repo: services.GitRepository,
                                        cloneService: CloneRepositoryService,
                                        statService: InsertionStatsService,
                                        protected val dbConfigProvider: DatabaseConfigProvider,
                                        fromDay: String, toDay: String)(implicit ec: ExecutionContext) extends Actor
  with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  val data = TableQuery[Tables.Metric]

  def dateOf(m: Metric): java.sql.Date = {
    println(m.day)
    val time = formatter.parse(m.day).getTime
    new java.sql.Date(time)
  }

  private def toMetricRow: List[Metric] => List[Tables.MetricRow] = xs =>
    xs.map(x => Tables.MetricRow(0, dateOf(x), Some(x.project), Some(x.developer), Some(x.metric), Some(x.value)))

  override def receive: Receive = {
    case Update => cloneService.exec(taskId, repo.repositoryUrl)
      .foreach { message =>
        context.parent ! Progress(message)
        context.parent ! Done(repo, analysis)
      }
  }

  import dbConfig.profile.api._

  private def analysis = {
    statService.exec(projectPath(taskId, repo.repositoryUrl), fromDay, toDay)
      .map(s => SegmentParser.parse(s.split("""\n""").toList))
      .map(toMetricRow)
      .map(rows => {
        rows.foreach(r => {
          dbConfig.db.run(Tables.Metric.insertOrUpdate(r))
        })
        rows.mkString(",")
      })

  }
}

object GitRepositoryUpdateJobWorker {

  def props(taskId: Int, r: services.GitRepository,
            clone: CloneRepositoryService,
            stat: InsertionStatsService,
            dbConfigProvider: DatabaseConfigProvider,
            fromDay: String, toDay: String)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobWorker(taskId, r, clone, stat, dbConfigProvider, fromDay, toDay))

}
