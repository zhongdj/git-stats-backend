package net.imadz.git.stats.graph.metabase

import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class HotFile(override val ws: WSClient, override val domain: String) extends CardGenerator with DatabaseMetadataProducer {
  override def template(project: String, branch: String): String =
    s"""
      |{
      |   "name":"${graphName(project, branch)}",
      |   "dataset_query":{
      |      "type":"native",
      |      "native":{
      |         "query":"SELECT COUNT(DISTINCT commit_id), file FROM git_commit WHERE project='$project' GROUP BY file ORDER BY COUNT(DISTINCT commit_id) DESC",
      |         "template-tags":{}
      |      },
      |      "database":#stats-db
      |   },
      |   "display":"table",
      |   "description":null,
      |   "visualization_settings":{
      |      "table.pivot_column": "count(distinct commit_id)",
      |      "table.cell_column":"file"
      |   }
      |}
      |""".stripMargin.replaceAll("\\n", "")

  override def contents: Future[Map[String, String]] =
  for {
    db <- findDB
  } yield Map(
    "#stats-db" -> (db \ "id").as[Int].toString,
  )

  override def graphName(project: String, branch: String): String = s"${shortName(project)}/$branch Hot Files"
}
