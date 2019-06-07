package net.imadz.git.stats.services

import java.io.File
import java.time.LocalDate

import sys.process._
import net.imadz.git.stats.{AppError, FileSystemError, ShellCommandExecError}

import scala.language.postfixOps

class CloneRepositoryService extends Constants {

  def exec(taskId: Long, repositoryUrl: String): Either[AppError, String] = {
    val taskDir = new File(s"${root}/${taskId}")
    val repoDir = new File(taskDir, projectOf(repositoryUrl))
    if (!repoDir.exists()) {
      init(repositoryUrl, repoDir)
    } else {
      if (repoDir.exists()) {
        update(repoDir)
      } else {
        init(repositoryUrl, repoDir)
      }
    }
  }

  private def init(repositoryUrl: String, repoDir: File) = {
    if (!repoDir.mkdirs()) Left(FileSystemError(s"cannot create folder: ${repoDir.getAbsolutePath}"))
    else
      try {
        Right(s"git clone ${repositoryUrl} ${repoDir.getAbsolutePath}" !!)
      } catch {
        case e: Throwable => Left(ShellCommandExecError(s"git clone ${repositoryUrl} ${repoDir.getAbsolutePath} with ${e.getMessage}"))
      }
  }

  private def update(repoDir: File): Either[AppError, String] = try {
    Right(s"""/opt/docker/git-update.sh ${repoDir.getAbsolutePath}""" !!)
  } catch {
    case e: Throwable => Left(ShellCommandExecError(s"cannot update repository at ${repoDir.getAbsolutePath} with ${e.getMessage}."))
  }
}
