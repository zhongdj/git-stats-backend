package net.imadz.git.stats.graph.metabase

import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WeeklyLocPerCommit(override val ws: WSClient, override val domain: String) extends CardGenerator with DatabaseMetadataProducer {
  override def template(project: String, branch: String): String =
    s"""
      |{
      |   "name":"${graphName(project, branch)}",
      |   "dataset_query":{
      |      "type":"native",
      |      "native":{
      |         "query":"SELECT * \nFROM v_commits_loc_by_week\n where \nproject='$project'",
      |         "template-tags":{
      |
      |         }
      |      },
      |      "database":#stats-db
      |   },
      |   "display":"line",
      |   "description":null,
      |   "visualization_settings":{
      |      "graph.dimensions":[
      |         "day"
      |      ],
      |      "graph.metrics":[
      |         "count",
      |         "net",
      |         "loc_per_commit"
      |      ],
      |      "series_settings":{
      |         "count":{
      |            "title":"Commits"
      |         },
      |         "net":{
      |            "title":"Net LoC"
      |         },
      |         "loc_per_commit":{
      |            "title":"LoC Per Commit"
      |         }
      |      },
      |      "graph.x_axis.axis_enabled":"rotate-45",
      |      "graph.show_values":false
      |   }
      |}
      |""".stripMargin.replaceAll("\\n", "")

  override def contents: Future[Map[String, String]] =
  for {
    db <- findDB
  } yield Map(
    "#stats-db" -> (db \ "id").as[Int].toString,
  )

  override def graphName(project: String, branch: String): String = s"${shortName(project)}/$branch Weekly Loc Per Commit"
}
