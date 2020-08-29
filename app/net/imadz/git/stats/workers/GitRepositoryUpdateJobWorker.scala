package net.imadz.git.stats.workers

import akka.actor.{ Actor, Props }
import net.imadz.git.stats.graph.metabase._
import net.imadz.git.stats.models.{ Metric, SegmentParser, Tables }
import net.imadz.git.stats.services._
import net.imadz.git.stats.workers.GitRepositoryUpdateJobMaster.{ Done, Progress, Update }
import net.imadz.git.stats.{ AppError, services }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.ws.WSClient
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.{ ExecutionContext, Future }

case class GitRepositoryUpdateJobWorker(taskId: Int, taskItemId: Int, repo: services.GitRepository,
    cloneService: CloneRepositoryService,
    statService: InsertionStatsService,
    functionStatsService: FunctionStatsService,
    taggedCommit: TaggedCommitStatsService,
    graphRepository: GraphRepository,
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
      cloneService.exec(taskId, repo.repositoryUrl, repo.branch, repo.local)
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
      .map(functionMetric)

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

  val graphService = new LayeredGraphCardGenerator(ws, "metabase:3000")
  val graphService2 = new DailyLocPerCommit(ws, "metabase:3000")
  val graphService3 = new WeeklyLocPerCommit(ws, "metabase:3000")
  val graphService4 = new HotFile(ws, "metabase:3000")

  val graphServices = graphService :: graphService2 :: graphService3 :: graphService4 :: Nil

  private def generateGraph: Future[String] => Future[String] = whatever =>
    Future.sequence(whatever :: graphServices.map(genGraph))
      .map(_.mkString("\n"))

  def find(taskId: Int, gName: String) = graphRepository.findGraph(taskId, gName)

  def save(taskId: Int, gName: String, card: Int) = graphRepository.createGraph(taskId, gName, card)

  private def genGraph: CardGenerator => Future[String] = gen => {
    val gName = gen.graphName(projectOf(repo.repositoryUrl), repo.branch)
    createGraph(gen, gName).recover {
      case e => s"$gName generation failed, ${e.getMessage}"
    }
  }

  private def createGraph(gen: CardGenerator, gName: String) = {
    for {
      idOpt <- find(taskId, gName)
      if idOpt.isEmpty
      card <- gen.generate(projectPath(taskId, repo.repositoryUrl), repo.branch)
      _ <- save(taskId, gName, card)
    } yield s"${gName} is created: id = ${idOpt.getOrElse(card)}"
  }

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

  private def functionMetric: Future[String] => Future[String] = for {
    got <- _
    funcMessage <- functionStatsService.exec(taskId, taskItemId, projectPath(taskId, repo.repositoryUrl), repo.profile, repo.excludes)
  } yield got + "\n" + funcMessage
}

object GitRepositoryUpdateJobWorker {

  def props(taskId: Int, taskItemId: Int, r: services.GitRepository,
    clone: CloneRepositoryService,
    stat: InsertionStatsService,
    funcStats: FunctionStatsService,
    taggedCommit: TaggedCommitStatsService,
    graphRepository: GraphRepository,
    ws: WSClient,
    dbConfigProvider: DatabaseConfigProvider,
    fromDay: String, toDay: String)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobWorker(taskId, taskItemId, r, clone, stat, funcStats, taggedCommit, graphRepository, ws, dbConfigProvider, fromDay, toDay))

}