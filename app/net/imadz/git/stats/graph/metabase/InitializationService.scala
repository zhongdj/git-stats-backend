package net.imadz.git.stats.graph.metabase

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.google.inject.Inject
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InitializationService @Inject() (ws: WSClient) {
  val domain: String = "metabase:3000"

  private def fetchSetupToken: Future[String] =
    ws.url(s"http://$domain/api/session/properties")
      .withHttpHeaders(("Content-Type", "application/json"))
      .get()
      .map(_.json)
      .map(js => (js \ "setup-token").as[String])

  private def setupAdmin(token: String): Future[String] =
    ws.url(s"http://$domain/api/setup")
      .withHttpHeaders(("Content-Type", "application/json"))
      .post(setupPayload(token))
      .map(_.json)
      .map(js => (js \ "id").as[String])

  private def setupPayload(token: String): String =
    s"""
       |{
       |	"token": "${token}",
       |	"prefs": {
       |		"site_name": "iMadz",
       |		"allow_tracking": "false"
       |	},
       |	"database": null,
       |	"user": {
       |		"first_name": "Barry",
       |		"last_name": "Zhong",
       |		"email": "zhongdj@gmail.com",
       |		"password": "1q2w3e4r5t",
       |		"site_name": "iMadz"
       |	}
       |}
       |""".stripMargin

  def execute(): Future[Unit] = {
    for {
      token <- fetchSetupToken
      id <- setupAdmin(token)
    } yield println(s"metabase setup with $id")
  }
}

object D3 extends App {

  import play.api.libs.ws.ahc._

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  val wsClient = AhcWSClient()
  new InitializationService(wsClient)
    .execute()
    .onComplete(println)
  while (true) {
    Thread.sleep(10000L)
  }
}
