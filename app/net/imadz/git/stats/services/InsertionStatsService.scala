package net.imadz.git.stats.services

import java.text.SimpleDateFormat
import java.time.LocalDate.parse
import java.time.Period

import net.imadz.git.stats.{ AppError, ShellCommandExecError }
import org.joda.time.{ DateTime, Days }

import scala.language.postfixOps
import scala.sys.process._

class InsertionStatsService extends Constants {

  def exec(project: String, fromStr: String, toStr: String): Either[AppError, String] = {
    println(s"InsertionStatsService: $fromStr, $toStr")
    range(fromStr, toStr)
      .map(stat(_, project))
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

  private def stat(date: String, p: String): Either[AppError, String] = {
    try {
      println(s"/opt/docker/stats-inserts.sh $p $date $date")
      Right(s"/opt/docker/stats-inserts.sh $p $date $date" !!)
    } catch {
      case e: Throwable => Left(ShellCommandExecError(s"stats-inserts failed with ${errorMessage(e)}"))
    }
  }
}
