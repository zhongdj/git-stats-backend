package net.imadz.git.stats.services

import java.time.LocalDate.parse

import net.imadz.git.stats.{ AppError, ShellCommandExecError }
import org.joda.time.{ DateTime, Days }

import scala.language.postfixOps
import scala.sys.process._

class ProductivityStatsService extends Constants {

  def excludeOnGitLog: List[String] => List[String] = xs => xs.map(x => s"""":(exclude)*$x*"""")

  def exec(project: String, fromStr: String, toStr: String, excludes: List[String]): Either[AppError, String] = {
    println(s"InsertionStatsService: $fromStr, $toStr, $excludeOnGitLog(excludes)")
    range(fromStr, toStr)
      .map(stat(_, project, excludeOnGitLog(excludes)))
      .foldLeft[Either[AppError, String]](Right(""))(accumulate)
  }

  private def accumulate: (Either[AppError, String], Either[AppError, String]) => Either[AppError, String] = {
    case (acc, stat) => acc.map(_ + "\n" + stat.fold(_.message, r => r))
  }

  private def range(fromStr: String, toStr: String) = {

    val toLocalDate = parse(toStr)
    val from = formatter.parse(fromStr)
    val to = formatter.parse(toStr)
    val days = Days.daysBetween(new DateTime(from.getTime), new DateTime(to.getTime)).getDays()
    println(fromStr, toStr, from, to, days)

    val dates = (0 to days).reverse.map(days => toLocalDate.minusDays(days)).map(_.toString)
    dates
  }

  private def errorMessage(e: Throwable) = {
    if (null != e.getCause) e.getCause.getMessage
    else e.getMessage
  }

  private def stat(date: String, p: String, excludes: List[String]): Either[AppError, String] = {
    try {
      println(s"/opt/docker/stats-inserts.sh $p $date $date")
      Right(
        (Seq("echo", s"$p", s"$date", s"$date") !!) +
          (("echo" :: excludes)
            #> Seq("xargs", "git", s"--git-dir=$p/.git", s"--work-tree=$p", "log", s"""--before="$date 23:59:59"""", s"""--after="$date 00:00:01"""", "--shortstat", "--pretty=%cE", "--", ".")
            #> Seq("/opt/docker/stats-delta.sh") !!)
      )
    } catch {
      case e: Throwable => Left(ShellCommandExecError(s"stats-inserts failed with ${errorMessage(e)}"))
    }
  }

}

object De extends App {
  def stat(date: String, p: String, excludes: List[String]): Either[String, String] = {
    try {
      Right(
        (Seq("echo", s"$p", s"$date", s"$date") !!) +
          (("echo" :: excludes)
            #> Seq("xargs", "git", s"--git-dir=$p/.git", s"--work-tree=$p", "log",
              s"""--before="$date 23:59:59"""", s"""--after="$date 00:00:01"""", "--shortstat", "--pretty=%cE", "--", ".")
              #> Seq("/Users/zhongdejian/Workspaces/git-stats-backend/scripts/stats-delta.sh") !!) + "\n")
    } catch {
      case e: Throwable => Left(s"stats-inserts failed with $e}")
    }
  }

  println(stat("2019-10-14", "/Users/zhongdejian/Workspaces/kunlun/metadata", List("""":(exclude)*thrift*"""", """":(exclude)*client*"""")).right)
  /*

  git --git-dir=/Users/zhongdejian/Workspaces/kunlun/metadata/.git --work-tree=/Users/zhongdejian/Workspaces/kunlun/metadata log --before='2019-10-15' --after='2018-10-14' --shortstat --pretty=%cE -- . ":(exclude)*thrift*"
  git --git-dir=/Users/zhongdejian/Workspaces/kunlun/metadata/.git --work-tree=/Users/zhongdejian/Workspaces/kunlun/metadata log --before='2019-10-15' --after='2018-10-14' --shortstat --pretty=%cE -- . ":\!*thrift*"

   */
}