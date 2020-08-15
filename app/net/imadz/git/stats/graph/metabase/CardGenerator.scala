package net.imadz.git.stats.graph.metabase

import scala.concurrent.{ ExecutionContext, Future }

trait CardGenerator extends MetabaseConfig with DatabaseMetadataProducer {
  self =>

  def graphName(project: String, branch: String): String

  def template(project: String, branch: String): String

  def contents: Future[Map[String, String]]

  def render(t: String, c: Map[String, String]): String = c.foldLeft(t) {
    case (acc, (key, value)) => acc.replaceAll(key, value)
  }

  def createCard(payload: String)(implicit context: ExecutionContext): Future[Int] = {
    println(payload)
    for {
      session <- sessionValue
      id <- ws.url("http://" + domain + "/api/card")
        .withHttpHeaders(("Cookie", session), ("Content-Type", "application/json"))
        .post[String](payload)
        .map(resp => resp.json)
        .map(_.\("id").as[Int])
    } yield id
  }

  def generate(project: String, branch: String)(implicit context: ExecutionContext): Future[Int] =
    for {
      c <- contents
      card <- createCard(render(template(project, branch), c))
    } yield card

  //  protected def domain = "localhost:3000"

}
