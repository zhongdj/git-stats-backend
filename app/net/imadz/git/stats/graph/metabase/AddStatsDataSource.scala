package net.imadz.git.stats.graph.metabase

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AddStatsDataSource @Inject() (override val ws: WSClient) extends MetabaseConfig {
  val domain: String = "metabase:3000"

  def execute(): Future[Unit] =
    for {
      session <- sessionValue
      r <- createDataSource(session)
    } yield println(r)

  val dataSourcePayload =
    """
      |{"engine":"mysql","name":"stats-db","details":{"host":"db","port":3306,"dbname":"stats","user":"root","password":"1q2w3e4r5t","ssl":false,"additional-options":null,"tunnel-enabled":false},"auto_run_queries":true,"is_full_sync":true,"schedules":{"cache_field_values":{"schedule_day":null,"schedule_frame":null,"schedule_hour":0,"schedule_type":"daily"},"metadata_sync":{"schedule_day":null,"schedule_frame":null,"schedule_hour":null,"schedule_type":"hourly"}}}
      |""".stripMargin

  private def createDataSource(session: String): Future[Unit] =
    ws.url(s"http://$domain/api/database")
      .withHttpHeaders(("Cookie", session), ("Content-Type", "application/json"))
      .post(dataSourcePayload)
      .map(_.json)
      .map(_ => ())

}

object D4 extends App {

  import play.api.libs.ws.ahc._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val wsClient = AhcWSClient()
  new AddStatsDataSource(wsClient)
    .execute()
    .onComplete(println)
  while (true) {
    Thread.sleep(10000L)
  }
}
