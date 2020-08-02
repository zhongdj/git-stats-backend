package net.imadz.git.stats.graph.metabase
import play.api.libs.json.{ JsArray, JsValue }

import scala.concurrent.{ ExecutionContext, Future }

trait DatabaseMetadataProducer {
  self: MetabaseConfig =>

  def domain: String

  protected def findDB(implicit context: ExecutionContext): Future[JsValue] =
    for {
      session <- sessionValue
      db <- ws
        .url(s"http://${domain}/api/database?include_tables=true")
        .withHttpHeaders(("Cookie", session), ("Content-Type", "application/json"))
        .get()
        .map(_.json)
        .map(_.as[JsArray].value.find(js => (js \ "name").as[String] == "stats-db").getOrElse(throw new RuntimeException("Cannot find stats-db")))
    } yield db

  protected def fetchTable(implicit context: ExecutionContext): Int => Future[JsValue] = id =>
    for {
      session <- sessionValue
      table <- ws.url(s"http://$domain/api/table/$id/query_metadata")
        .withHttpHeaders(("Cookie", session), ("Content-Type", "application/json"))
        .get()
        .map(_.json)
    } yield table

  protected def findTable(db: JsValue, tableName: String)(implicit context: ExecutionContext): Future[JsValue] = {
    (db \ "tables").as[JsArray].value.find(js => {
      (js \ "name").as[String] == tableName
    })
      .map(js => (js \ "id").as[Int])
      .map(fetchTable)
      .getOrElse(Future.failed(new RuntimeException(s"cannot find ${tableName} table")))
  }
}
