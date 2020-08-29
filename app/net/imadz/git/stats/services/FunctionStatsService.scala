package net.imadz.git.stats.services

import com.google.inject.Inject
import net.imadz.git.stats.services.GolangFuncsParser.{ FuncMetric, parseFunctionMetrics }

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.sys.process._

case class FunctionStatsService @Inject() (repository: FunctionMetricsRepository) {

  def map(lines: String): List[FuncMetric] = parseFunctionMetrics(lines)

  def save(taskId: Int, funcMetrics: List[FuncMetric]): Future[List[Int]] = repository.save(taskId, funcMetrics)

  def exec(taskId: Int, projectPath: String, profile: Option[String], excludes: List[String]): Future[String] = {
    println("processing function analysis")
    val str = scanFunctions(projectPath)
    for {
      _ <- save(taskId, map(str))
    } yield str
  }

  private def scanFunctions(projectPath: String) = {
    s"/opt/docker/golang-longfuncs.sh $projectPath" !!
  }
}
