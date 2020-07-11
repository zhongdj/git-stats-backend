package net.imadz.git.stats.graph.metabase
import scala.concurrent.Future

class LayeredGraphCardGenerator extends CardGenerator {
  override def template(project: String): String =
    s"""
      |{
      |   "name":"Controllers Distributed Graph2",
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
      |               "controllers"
      |            ]
      |         ],
      |         "breakout":[
      |            [
      |               "datetime-field",
      |               [
      |                  "field-id",
      |                  #tagged_commit.day
      |               ],
      |               "day"
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
      |      ]
      |   }
      |
      |}
      |""".stripMargin

  override def contents: Map[String, String] = Map(
    "#stats-db"-> "2",
    "#tagged_commit.project" -> "38",
    "#tagged_commit.tag" -> "37",
    "#tagged_commit.day" -> "40",
    "#tagged_commit.commit_id" -> "42"
  )

  override def render(t: String, c: Map[String, String]): String = c.foldLeft(t){
    case (acc, (key, value)) => acc.replaceAll(key, value)
  }

  override def createCard(payload: String): Future[Int] = {
    println(payload)
    Future.successful(1)




    
  }
}

object D2 extends App {
  new LayeredGraphCardGenerator().generate("/root/.tasks/1/git-stats-backend")
}