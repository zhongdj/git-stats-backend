package net.imadz.git.stats

trait AppError {
  def message: String
}

case class FileSystemError(message: String) extends AppError

case class ShellCommandExecError(message: String) extends AppError
