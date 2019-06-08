package controllers

import java.text.SimpleDateFormat
import java.util.Date

import javax.inject._
import net.imadz.git.stats.models.{ Metric, SegmentParser, Tables }
import net.imadz.git.stats.services._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.libs.json.Json
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
    clone: CloneRepositoryService, stat: InsertionStatsService,
    create: CreateTaskService,
    protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends AbstractController(cc) with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  val data = TableQuery[Tables.Metric]

  def dateOf(m: Metric): java.sql.Date = {
    println(m.day)
    val time = formatter.parse(m.day).getTime
    new java.sql.Date(time)
  }
  def toMetricRow: List[Metric] => List[Tables.MetricRow] = xs =>
    xs.map(x => Tables.MetricRow(0, dateOf(x), Some(x.project), Some(x.developer), Some(x.metric), Some(x.value)))

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
      .exec(711L, l)
      .fold(_.message, r => r)
      + stat
      .exec(projectPath(711L, l), "2019-06-01", "2019-06-08")
      .map(s => SegmentParser.parse(s.split("""\n""").toList))
      .map(toMetricRow)
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

}
