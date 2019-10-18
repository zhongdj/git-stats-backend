package net.imadz.git.stats.services

import java.text.SimpleDateFormat

trait Constants {
  val root = "/root/.tasks"
  val r = """.*/(.*?)(?:\.git)?""".r

  def projectOf(repositoryUrl: String): String = repositoryUrl match {
    case r(projectName) => projectName
  }

  def projectPath(taskId: Long, repositoryUrl: String): String = {
    s"${root}/${taskId}/${projectOf(repositoryUrl)}"
  }
  val formatter = new SimpleDateFormat("yyyy-MM-dd")
  def dateOf(m: String): java.sql.Date = {
    val time = formatter.parse(m).getTime
    new java.sql.Date(time)
  }
}
