package net.imadz.git.stats.services

trait Constants {
  val root = "/root/.tasks"
  val r = """.*/(.*).git""".r

  def projectOf(repositoryUrl: String): String = repositoryUrl match {
    case r(projectName) => projectName
  }

  def projectPath(taskId: Long, repositoryUrl: String): String = {
    s"${root}/${taskId}/${projectOf(repositoryUrl)}"
  }
}
