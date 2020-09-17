package net.imadz.git.stats.services

import java.io.File
import java.sql.Date
import java.util.UUID

import akka.actor.ActorRef
import com.google.inject.Inject
import net.imadz.git.stats.{ AppError, ShellCommandExecError }
import net.imadz.git.stats.services.GolangFuncsParser.{ FuncMetric, parseFunctionMetrics }
import net.imadz.git.stats.workers.FileIndexActor.{ Found, SearchFile }

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.sys.process._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._

case class FunctionStatsService @Inject() (repository: FunctionMetricsRepository, deltaService: CalculateFuncMetricDeltaService) {

  private def map(lines: String): List[FuncMetric] = {
    val r = parseFunctionMetrics(lines)
    println("function metrics size: " + r.size)
    r
  }

  private def findFilePath(fileIndex: ActorRef): FuncMetric => Future[FuncMetric] = metric => {
    implicit val timeout: Timeout = Timeout(5 seconds)
    val head = metric.abbrPath.split(":").head
    println(s"processing abbr path: ${metric.abbrPath} to $head")
    fileIndex ? SearchFile(head) map {
      case Found(file) => metric.copy(fullPath = file)
      case _           => metric
    }
  }

  private def refineMetrics(fileIndex: ActorRef, funcMetrics: List[FuncMetric]): Future[List[FuncMetric]] = Future.sequence(
    funcMetrics.map(findFilePath(fileIndex))
  )

  private def save(fileIndex: ActorRef, taskId: Int, taskItemId: Int, projectRoot: String, theDay: Date, funcMetrics: List[FuncMetric]): Future[List[Int]] =
    for {
      refinedFuncMetrics <- refineMetrics(fileIndex, funcMetrics)
      ids <- repository.save(taskId, taskItemId, projectRoot, theDay, refinedFuncMetrics)
    } yield ids

  def exec(fileIndex: ActorRef, taskId: Int, taskItemId: Int, projectPath: String, theDay: Date, commitId: String, profile: Option[String], excludes: List[String]): Future[String] = {
    println("processing function analysis: ")
    println(s"taskId = $taskId, taskItemId = $taskItemId, projectPath = $projectPath theDay = $theDay, commitId = $commitId")
    val str = scanFunctions(projectPath, commitId, excludes)
    println(str)
    val r = exeSave(fileIndex, taskId, taskItemId, projectPath, theDay, str).recover {
      case e: Throwable =>
        println("funcs save failed")
        e.printStackTrace()
        str + "\n" + e.getMessage
    }
    r.onComplete(_ => deltaService.exec(taskId, taskItemId))
    r
  }

  private def exeSave(fileIndex: ActorRef, taskId: Int, taskItemId: Int, projectPath: String, theDay: Date, str: String) = {
    for {
      ids <- save(fileIndex, taskId, taskItemId, projectPath, theDay, map(str))
    } yield {
      println("funcs metric saved")
      ids.foreach(println)
      str
    }
  }

  private def retreat(repoDir: File, commitId: String): Either[AppError, String] = try {
    Right(s"""/opt/docker/git-retreat.sh ${repoDir.getAbsolutePath} ${commitId}""" !!)
  } catch {
    case e: Throwable => Left(ShellCommandExecError(s"cannot retreat repository at ${repoDir.getAbsolutePath} with ${e.getMessage}."))
  }

  def scanFunctions(projectPath: String, commitId: String, excludes: List[String]) = {
    retreat(new File(projectPath), commitId).fold(println, println)

    val cmd = if (excludes.nonEmpty) {
      val ignores = excludes.mkString("""'""", "|", """'""")
      s"""
         |cd ${projectPath} && golongfuncs -top 300 +lines +in_params +complexity +complexity/lines -ignore $ignores
         |""".stripMargin
    } else {
      s"""
         |cd ${projectPath} && golongfuncs -top 300 +lines +in_params +complexity +complexity/lines
         |""".stripMargin
    }

    println(cmd)

    val shellFile = new File("/tmp", UUID.randomUUID().toString + ".sh")
    writeFile(shellFile.getAbsolutePath, cmd)
    println(s"${shellFile.getName}, written")
    scanFunction(projectPath, shellFile.getName)

  }

  import java.io._

  /**
   * write a `Seq[String]` to the `filename`.
   */
  def writeFile(filename: String, lines: Seq[String]): Unit = {
    val file = new File(filename)
    val bw = new BufferedWriter(new FileWriter(file))
    for (line <- lines) {
      bw.write(line)
    }
    bw.close()
  }

  /**
   * write a `String` to the `filename`.
   */
  def writeFile(filename: String, s: String): Unit = {
    val file = new File(filename)
    file.setExecutable(true)
    if (file.exists()) {
      file.delete()
    }
    file.createNewFile()
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write(s)
    bw.close()
  }

  private def scanFunction(projectPath: String, shellFilePath: String) = {
    s"/opt/docker/golang-function-analysis.sh $projectPath $shellFilePath" !!
  }
}
