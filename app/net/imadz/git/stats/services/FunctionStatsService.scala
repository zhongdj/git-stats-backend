package net.imadz.git.stats.services

import com.google.inject.Inject
import net.imadz.git.stats.services.GolangFuncsParser.{ FuncMetric, parseFunctionMetrics }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.sys.process._

case class FunctionStatsService @Inject() (repository: FunctionMetricsRepository) {

  def map(lines: String): List[FuncMetric] = {
    val r = parseFunctionMetrics(lines)
    println("function metrics size: " + r.size)
    r
  }

  def save(taskId: Int, taskItemId: Int, projectRoot: String, funcMetrics: List[FuncMetric]): Future[List[Int]] =
    repository.save(taskId, taskItemId, projectRoot, funcMetrics)

  def exec(taskId: Int, taskItemId: Int, projectPath: String, profile: Option[String], excludes: List[String]): Future[String] = {
    println("processing function analysis: ")
    println(s"taskId = $taskId, taskItemId = $taskItemId, projectPath = $projectPath")
    val str = scanFunctions(projectPath)
    println(str)

    {
      for {
        ids <- save(taskId, taskItemId, projectPath, map(str))
      } yield {
        println("funcs metric saved")
        ids.foreach(println)
        str
      }
    }.recover {
      case e: Throwable =>
        println("funcs save failed")
        e.printStackTrace()
        str + "\n" + e.getMessage
    }
  }

  private def scanFunctions(projectPath: String) = {
    s"/opt/docker/golang-function-analysis.sh $projectPath" !!
  }
}
