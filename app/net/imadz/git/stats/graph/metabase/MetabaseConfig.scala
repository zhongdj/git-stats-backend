package net.imadz.git.stats.graph.metabase

import java.io.{ File, FileReader }
import java.util.Properties

import play.api.libs.ws.WSClient

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait MetabaseConfig {
  def ws: WSClient

  def loginPayload(username: String, password: String): String =
    s"""
       |{"password":"${password}","username":"${username}","remember":true}
       |""".stripMargin

  def config(filePath: String = "conf/metabase/session.properties"): Map[String, String] = {
    val prop = new Properties()
    prop.load(new FileReader(new File(filePath)))
    prop.entrySet().asScala.map(entry => (entry.getKey.asInstanceOf[String], entry.getValue.asInstanceOf[String])).toMap[String, String]
  }

  lazy val sessionValue: Future[String] = {
    ws.url("http://metabase:3000/api/session")
      .withHttpHeaders(("Content-Type", "application/json"))
      .post(loginPayload(config()("username"), config()("password")))
      .map(resp => resp.headerValues("Set-Cookie"))
      .map(setCookies => setCookies.find(_.startsWith("metabase.SESSION="))
        .map { setCookie =>
          val r = setCookie.substring(0, setCookie.indexOf(";"))
          println(r)
          r
        }
        .getOrElse(throw new RuntimeException("invalid username/password"))
      )
  }
  protected def shortName(project: String): String = {
    project.substring(project.lastIndexOf("/") + 1)
  }
}
