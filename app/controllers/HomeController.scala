package controllers

import java.sql.Date
import java.time.{ LocalDate, Period }

import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.Date
import javax.inject._
import net.imadz.git.stats.models.{ Metric, SegmentParser, Tables }
import net.imadz.git.stats.services.{ CloneRepositoryService, Constants, InsertionStatsService }
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.mvc._
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject() (cc: ControllerComponents, clone: CloneRepositoryService, stat: InsertionStatsService, protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) extends AbstractController(cc) with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  val data = TableQuery[Tables.ProductivityData]

  def dateOf(m: Metric): java.sql.Date = {
    println(m.day)
    val period = LocalDate.parse(m.day)
    new java.sql.Date(period.getYear - 1000, period.getMonthValue - 1, period.getDayOfMonth - 1)
  }

  def toProductivityDataRow: List[Metric] => List[Tables.ProductivityDataRow] = xs =>
    xs.map(x => Tables.ProductivityDataRow(0, dateOf(x), Some(x.project), Some(x.developer), Some(x.metric), Some(x.value)))

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
      .map(toProductivityDataRow)
      .map(rows => {
        rows.foreach(r => {
          //          val query = Tables.ProductivityData += r
          val query = Tables.ProductivityData.insertOrUpdate(r)
          dbConfig.db.run(query)
        })
        rows.mkString(",")
      })
      .fold(_.message, r => r)

    )
  }
}
