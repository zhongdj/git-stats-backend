package net.imadz.git.stats.workers

import akka.actor.{ Actor, Props }
import net.imadz.git.stats.models.InvertedFileIndex
import net.imadz.git.stats.workers.FileIndexActor._

class FileIndexActor(rootPath: String) extends Actor {
  var fileIndex: InvertedFileIndex = InvertedFileIndex(rootPath)

  override def receive: Receive = {
    case AddFile(fullFilePath) =>
      println(s"add file: $fullFilePath")
      fileIndex = fileIndex + fullFilePath
      context.sender() ! Done
    case SearchFile(matchingFilePath) =>
      println(s"search file: $matchingFilePath")
      context.sender() ! fileIndex.search(matchingFilePath)
        .map(file => Found(file.getAbsolutePath))
        .getOrElse(NotFound)
  }
}

object FileIndexActor {

  def apply(rootPath: String): Props = Props(new FileIndexActor(rootPath))

  case class AddFile(fullFilePath: String)

  case object Done

  case class SearchFile(matchingFilePath: String)

  sealed trait SearchResult

  case class Found(fullFilePath: String) extends SearchResult

  case object NotFound extends SearchResult

}