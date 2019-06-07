package controllers

import javax.inject._
import net.imadz.git.stats.models.SegmentParser
import net.imadz.git.stats.services.{CloneRepositoryService, Constants, InsertionStatsService}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, clone: CloneRepositoryService, stat: InsertionStatsService
                               , protected val dbConfigProvider: DatabaseConfigProvider
                              )(implicit ec: ExecutionContext) extends AbstractController(cc) with Constants with HasDatabaseConfigProvider[JdbcProfile] {

  // val companies = TableQuery[ProductivityData]
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
      .fold(_.message, r => r)

    )
  }
}
