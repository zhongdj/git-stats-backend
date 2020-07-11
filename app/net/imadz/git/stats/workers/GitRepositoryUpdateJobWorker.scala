package net.imadz.git.stats.workers

import akka.actor.{ Actor, Props }
import net.imadz.git.stats.graph.metabase.LayeredGraphCardGenerator
import net.imadz.git.stats.models.{ Metric, SegmentParser, Tables }
import net.imadz.git.stats.services.{ CloneRepositoryService, Constants, InsertionStatsService, TaggedCommitStatsService }
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster.{ Done, Progress, Update }
import net.imadz.git.stats.{ AppError, services }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.ws.WSClient
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.{ ExecutionContext, Future }

case class GitRepositoryUpdateJobWorker(taskId: Int, repo: services.GitRepository,
    cloneService: CloneRepositoryService,
    statService: InsertionStatsService,
    taggedCommit: TaggedCommitStatsService,
    ws: WSClient,
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
      cloneService.exec(taskId, repo.repositoryUrl, repo.branch)
        .map { message =>
          parent ! Progress(message)
          analysis.fold(
            e => parent ! Done(repo, Left(e)),
            f => f.foreach(message => parent ! Done(repo, Right(message))))
        }
  }

  private def analysis = {
    fetchRepository
      .map(parseMetric)
      .map(toMetricRow(taskId))
      .map(insertMetric)
      .map(tagCommit)
      .map(generateGraph)

  }

  private def fetchRepository: Either[AppError, String] =
    statService.exec(projectPath(taskId, repo.repositoryUrl), fromDay, toDay, repo.excludes)

  private def parseMetric: String => List[Metric] =
    s => SegmentParser.parse(s.split("""\n""").toList)

  private def insertMetric: List[Tables.MetricRow] => Future[String] = rows => {
    import dbConfig.profile.api._
    dbConfig.db.run(DBIO.sequence(rows.map(Tables.Metric.insertOrUpdate)))
      .map(_ => rows.mkString(","))
      .recover {
        case e =>
          e.printStackTrace()
          rows.mkString(",")
      }
  }

  private def generateGraph: Future[String] => Future[String] = whatever =>
    for {
      last <- whatever
      graphService = new LayeredGraphCardGenerator(ws, "metabase:3000")
      graphId <- graphService.generate(projectPath(taskId, repo.repositoryUrl), repo.branch)
    } yield last + s"graphGenerated: $graphId"

  private def tagCommit: Future[String] => Future[String] = {
    futureStr =>
      futureStr.flatMap { str =>
        println(s"tagging: ${projectPath(taskId, repo.repositoryUrl)} commit started: ")
        val eventualStr2 = taggedCommit.exec(projectPath(taskId, repo.repositoryUrl), repo.profile, repo.excludes)
          .fold(
            e => Future.successful(s"Failed to tag commit with: ${e.message}"),
            s => s
          )
        eventualStr2.map(str2 => str + "\n" + str2)
      }
  }
}

object GitRepositoryUpdateJobWorker {

  def props(taskId: Int, r: services.GitRepository,
    clone: CloneRepositoryService,
    stat: InsertionStatsService,
    taggedCommit: TaggedCommitStatsService,
    ws: WSClient,
    dbConfigProvider: DatabaseConfigProvider,
    fromDay: String, toDay: String)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobWorker(taskId, r, clone, stat, taggedCommit, ws, dbConfigProvider, fromDay, toDay))

}