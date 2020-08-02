package net.imadz.git.stats.graph.metabase

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import play.api.libs.json.{ JsArray, JsValue }
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LayeredGraphCardGenerator(override val ws: WSClient, override val domain: String) extends CardGenerator {

  override def template(project: String, branch: String): String =
    s"""
       |{
       |   "name":"${shortName(project)}/$branch Onion Architecture Layered Graph",
       |   "dataset_query":{
       |      "query":{
       |         "source-table":5,
       |         "filter":[
       |            "and",
       |            [
       |               "=",
       |               [
       |                  "field-id",
       |                  #tagged_commit.project
       |               ],
       |               "$project"
       |            ],
       |            [
       |               "=",
       |               [
       |                  "field-id",
       |                  #tagged_commit.tag
       |               ],
       |               "application",
       |               "domain",
       |               "infrastructure"
       |            ]
       |         ],
       |         "breakout":[
       |            [
       |               "datetime-field",
       |               [
       |                  "field-id",
       |                  #tagged_commit.day
       |               ],
       |               "week"
       |            ],
       |            [
       |               "field-id",
       |               #tagged_commit.tag
       |            ]
       |         ],
       |         "aggregation":[
       |            [
       |               "distinct",
       |               [
       |                  "field-id",
       |                  #tagged_commit.commit_id
       |               ]
       |            ]
       |         ]
       |      },
       |      "type":"query",
       |      "database": #stats-db
       |   },
       |   "display":"area",
       |   "description":null,
       |   "visualization_settings":{
       |      "graph.dimensions":[
       |         "day",
       |         "tag"
       |      ],
       |      "graph.metrics":[
       |         "count"
       |      ],
       |      "stackable.stack_type" : "stacked"
       |   }
       |
       |}
       |""".stripMargin.replaceAll("\\n", "")

  private def findTaggedTable(db: JsValue): Future[JsValue] =
    findTable(db, "tagged_commit")

  private def project(taggedCommit: JsValue): String = findColumn(taggedCommit, "project")

  private def tag(taggedCommit: JsValue): String = findColumn(taggedCommit, "tag")

  private def day(taggedCommit: JsValue): String = findColumn(taggedCommit, "day")

  private def commit(taggedCommit: JsValue): String = findColumn(taggedCommit, "commit_id")

  private def findColumn(taggedCommit: JsValue, columnName: String) = {
    (taggedCommit \ "fields").as[JsArray].value.find(js => {
      (js \ "name").as[String] == columnName
    }).map(js => (js \ "id").as[Int].toString)
      .getOrElse(throw new RuntimeException(s"cannot find $columnName"))
  }

  private def fetchColumns(taggedCommit: JsValue): Future[(String, String, String, String)] = Future.successful(
    (project(taggedCommit), tag(taggedCommit), day(taggedCommit), commit(taggedCommit))
  )

  override def contents: Future[Map[String, String]] =
    for {
      db <- findDB
      taggedCommit <- findTaggedTable(db)
      (projectColumnId, tagColumnId, dayColumnId, commitColumnId) <- fetchColumns(taggedCommit)
    } yield Map(
      "#stats-db" -> (db \ "id").as[Int].toString,
      "#tagged_commit.project" -> projectColumnId,
      "#tagged_commit.tag" -> tagColumnId,
      "#tagged_commit.day" -> dayColumnId,
      "#tagged_commit.commit_id" -> commitColumnId
    )

}

object D2 extends App {

  import play.api.libs.ws.ahc._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  val wsClient = AhcWSClient()
  new LayeredGraphCardGenerator(wsClient, "metabase:3000")
    .generate("/root/.tasks/1/tweet", "master")
    .onComplete(println)
  while (true) {
    Thread.sleep(10000L)
  }
}