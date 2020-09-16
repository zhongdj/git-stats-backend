package net.imadz.git.stats.workers

import java.sql.Date

import akka.actor.{ Actor, ActorRef, Props }
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
import scala.language.postfixOps
import scala.sys.process._

case class GitRepositoryUpdateJobWorker(taskId: Int, taskItemId: Int, repo: services.GitRepository,
    cloneService: CloneRepositoryService,
    productivityStatsService: ProductivityStatsService,
    functionStatsService: FunctionStatsService,
    taggedCommitStatsService: TaggedCommitStatsService,
    graphRepository: GraphRepository,
    ws: WSClient,
    protected val dbConfigProvider: DatabaseConfigProvider,
    fromDay: String, toDay: String)(implicit ec: ExecutionContext) extends Actor
  with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  val fileIndex: ActorRef = context.actorOf(FileIndexActor("/"), "taskItemId")
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
            f => f.foreach(message => context.parent ! Done(repo, Right(message))))
        }
  }

  private def analysis: Either[AppError, Future[String]] = {
    productivityMetric.map { productivityMetrics =>
      for {
        s1 <- productivityMetrics
        s2 <- tagCommits
        s3 <- processCyclomaticAnalysis
        s4 <- generateGraph
      } yield s"$s1 \n $s2 \n $s3 \n $s4"
    }
  }

  private def productivityMetric = {
    fetchRepository
      .map(parseMetric)
      .map(toMetricRow(taskId))
      .map(insertMetric)
  }

  private def fetchRepository: Either[AppError, String] =
    productivityStatsService.exec(projectPath(taskId, repo.repositoryUrl), fromDay, toDay, repo.excludes)

  private def parseMetric: String => List[Metric] =
    s =>
      SegmentParser.parse(s.split("""\n""").toList)

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

  import dbConfig.profile.api._

  def eventualDays(): Future[List[(Date, String)]] = {
    println(s"eventual Days: starting ${projectPath(taskId, repo.repositoryUrl)}.....................................................")
    val q = (for {
      commit <- Tables.GitCommit.sortBy(_.day.desc)
      if commit.project === projectPath(taskId, repo.repositoryUrl)
    } yield commit).groupBy(_.day).map {
      case (day, group) => (day, group.map(_.commitId).max.get)
    }.result.map(_.toList)
    dbConfig.db.run(q)
  }

  private def align(projectRoot: String, branch: String): Future[String] = Future.successful {
    println(s"align: $projectRoot/$branch")
    s"/opt/docker/git-replay.sh $projectRoot $branch" !!
  }

  private def processCyclomaticAnalysis = {
    for {
      dayCommits <- eventualDays()
      _ <- align(projectPath(taskId, repo.repositoryUrl), repo.branch)
      _ = dayCommits.foreach(println)
      f = dayCommits.map(dc => functionMetric(dc._1, dc._2))
        .foldLeft[Future[String] => Future[String]](itself)((g, f) => g.compose(f))
      message <- f(Future.successful(""))
    } yield message
  }

  def itself[T]: T => T = it => it

  val graphService = new LayeredGraphCardGenerator(ws, "metabase:3000")
  val graphService2 = new DailyLocPerCommit(ws, "metabase:3000")
  val graphService3 = new WeeklyLocPerCommit(ws, "metabase:3000")
  val graphService4 = new HotFile(ws, "metabase:3000")

  val graphServices = graphService :: graphService2 :: graphService3 :: graphService4 :: Nil

  private def generateGraph: Future[String] =
    Future.sequence(graphServices.map(genGraph))
      .map(_.mkString(", "))

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

  private def tagCommits: Future[String] = {
    println(s"tagging: ${projectPath(taskId, repo.repositoryUrl)} commit started: ")
    taggedCommitStatsService.exec(fileIndex, projectPath(taskId, repo.repositoryUrl), repo.profile, repo.excludes)
      .fold(
        e => Future.successful(s"Failed to tag commit with: ${e.message}"),
        s => s
      )
  }

  private def functionMetric(theDate: Date, theCommit: String): Future[String] => Future[String] = for {
    got <- _
    funcMessage <- functionStatsService.exec(fileIndex, taskId, taskItemId, projectPath(taskId, repo.repositoryUrl), theDate, theCommit, repo.profile, repo.excludes)
  } yield got + "\n" + funcMessage
}

object GitRepositoryUpdateJobWorker {

  def props(taskId: Int, taskItemId: Int, r: services.GitRepository,
    clone: CloneRepositoryService,
    stat: ProductivityStatsService,
    funcStats: FunctionStatsService,
    taggedCommit: TaggedCommitStatsService,
    graphRepository: GraphRepository,
    ws: WSClient,
    dbConfigProvider: DatabaseConfigProvider,
    fromDay: String, toDay: String)(implicit ec: ExecutionContext): Props =
    Props(new GitRepositoryUpdateJobWorker(taskId, taskItemId, r, clone, stat, funcStats, taggedCommit, graphRepository, ws, dbConfigProvider, fromDay, toDay))

}