package net.imadz.git.stats.graph.metabase

import scala.concurrent.Future

trait CardGenerator {

  def generate(project: String): Future[Int] =
    createCard(render(template(project), contents))

  def template(project: String): String

  def contents: Map[String, String]

  def render(t: String, c: Map[String, String]): String

  def createCard(payload: String): Future[Int]
}
