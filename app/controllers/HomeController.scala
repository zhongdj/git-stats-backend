package controllers

import java.util.Date

import javax.inject._
import net.imadz.git.stats.graph.metabase.{ AddStatsDataSource, InitializationService }
import net.imadz.git.stats.models.{ Metric, SegmentParser, Tables }
import net.imadz.git.stats.services._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.{ ExecutionContext, Future }

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (
    cc: ControllerComponents,
    ws: WSClient,
    clone: CloneRepositoryService, stat: ProductivityStatsService,
    functionStat: FunctionStatsService,
    deltaService: CalculateFuncMetricDeltaService,
    create: CreateTaskService,
    metabaseInit: InitializationService,
    addDataSource: AddStatsDataSource,
    protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends AbstractController(cc) with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  for {
    _ <- metabaseInit.execute()
    _ <- addDataSource.execute()
  } yield ()

  val data = TableQuery[Tables.Metric]

  def toMetricRow(taskId: Long): List[Metric] => List[Tables.MetricRow] = xs =>
    xs.map(x => Tables.MetricRow(0, taskId, dateOf(x.day), Some(x.project), Some(x.developer), Some(x.metric), Some(x.value)))

  import dbConfig.profile.api._

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    val l = "git@github.com:zhongdj/git-stats-backend.git"
    Ok(clone
      .exec(711L, l, "master")
      .fold(_.message, r => r)
      + stat
      .exec(projectPath(711L, l), "2019-06-01", "2019-06-08", Nil)
      .map(s => SegmentParser.parse(s.split("""\n""").toList))
      .map(toMetricRow(711L))
      .map(rows => {
        rows.foreach(r => {
          val query = Tables.Metric.insertOrUpdate(r)
          dbConfig.db.run(query)
        })
        rows.mkString(",")
      })
      .fold(_.message, r => r)
    )
  }

  def today: String = formatter.format(new Date)

  def createTask() = Action.async(parse.json) { request =>
    request.body.validate[CreateTaskReq]
      .map(req => create.exec(req.repositories, req.fromDay, req.toDay.getOrElse(today)))
      .fold(
        e => Future.successful(BadRequest(e.mkString(","))),
        eventuateResp => eventuateResp.map(r => Ok(Json.toJson(r)))
      )
  }

  def delta(taskId: Int, taskItemId: Int): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    deltaService.exec(taskId, taskItemId)
      .map(_ => Ok("completed"))
  }

}
