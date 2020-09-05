package net.imadz.git.stats.services

import java.io.File

import com.google.inject.Inject
import net.imadz.git.stats.common.shell._
import net.imadz.git.stats.{AppError, ShellCommandExecError}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.sys.process._


case class CommitOnFile(id: String, file: String, developer: String, date: String, message: String)

case class TaggedCommit(tag: String, commit: CommitOnFile)

class TaggedCommitStatsService @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext)
  extends Traversal with Constants with HasDatabaseConfigProvider[JdbcProfile] with Tagged {

  def exec(dir: String, profile: Option[String], excludes: List[String]): Either[AppError, Future[String]] = {
    val files: Either[AppError, List[String]] = lsTreeObjects(dir, excludes)
    val commits: Either[AppError, List[CommitOnFile]] = files.flatMap(fs => sequence(fs.map(readCommits(dir)))).map(_.flatten)
    commits.map { cs =>
      val eventualSavedCommits = batchInsertOrUpdate(dir)(cs)
      val eventualTaggedCommits = Future.successful(taggingCommits(profile, cs)).flatMap(batchInsertOrUpdateTaggedCommit(dir))
      for {
        _ <- eventualSavedCommits
        taggedCommits <- eventualTaggedCommits
      } yield taggedCommits.mkString(",")
    }
  }

  private def taggingCommits(profile: Option[String], cs: List[CommitOnFile]) = {
    cs.flatMap(tagging(profile))
  }

  private def lsTreeObjects(dir: String, excludes: List[String]): Either[AppError, List[String]] = try {
    val cli = s"""/opt/docker/git-ls-tree.sh $dir"""
    println(s"$cli")
    Right(excludeByGrep(excludes)(cli) !!)
      .map(toMultipleLines)
  } catch {
    case e: Throwable => Left(ShellCommandExecError(s"Failed to execute /opt/docker/git-ls-tree.sh $dir with: ${e.getMessage}"))
  }

  private def readCommits(dir: String): String => Either[AppError, List[CommitOnFile]] = file => try {
    println(s"/opt/docker/git-file-log.sh $dir $file")
    Right(s"/opt/docker/git-file-log.sh $dir $file" !!)
      .map(parseCommitOnFile(file))
  } catch {
    case e: Throwable => Left(ShellCommandExecError(s"Failed to execute: /opt/docker/git-file-log.sh $dir $file with: ${e.getMessage}"))
  }

  private def parseCommitOnFile(file: String): String => List[CommitOnFile] = CommitParser.read(file)

  private def tagging(profile: Option[String]): CommitOnFile => List[TaggedCommit] = commit => tags(profile)(commit).map(tag => TaggedCommit(tag, commit))

  import dbConfig.profile.api._
  import net.imadz.git.stats.models.Tables

  private def batchInsertOrUpdate(dir: String)(xs: List[CommitOnFile]): Future[List[CommitOnFile]] = {
    val commits = xs.map(x => Tables.GitCommitRow(0, dir, x.id, x.file, x.developer, dateOf(x.date), x.message))
    Future.sequence(commits.map(commit => dbConfig.db.run(Tables.GitCommit.insertOrUpdate(commit))))
      .map(_ => xs)
  }

  private def batchInsertOrUpdateTaggedCommit(dir: String)(xs: List[TaggedCommit]): Future[List[TaggedCommit]] = {
    val commits = xs.map(x => Tables.TaggedCommitRow(0, dateOf(x.commit.date), x.tag, dir, x.commit.id, x.commit.file))
    Future.sequence(commits.map(commit => dbConfig.db.run(Tables.TaggedCommit.insertOrUpdate(commit))))
      .map(_ => xs)
  }

  private def tags(profile: Option[String]): CommitOnFile => List[String] = commit =>
    allTaggers(profile.getOrElse("java.onion"))(commit.file)

  private lazy val allTaggers = (builtInTaggers ++ customTaggers).withDefaultValue(javaOnionTagger)
  private val javaOnionTagger: Tagger = taggerOf("conf/tags/java.onion.profile.properties")
  private val golangTagger: Tagger = taggerOf("conf/tags/golang.profile.properties")
  private val builtInTaggers: Map[String, Tagger] = Map(
    "java.onion" -> javaOnionTagger,
    "golang" -> golangTagger,
  )
  private lazy val customTaggers: Map[String, Tagger] = new File("/root/tags")
    .listFiles()
    .toList
    .map(file => profileName(file.getName) -> taggerOf(file))
    .toMap

  private def profileName(fileName: String): String = {
    val suffix = ".profile.properties"
    if (fileName.endsWith(suffix)) {
      fileName.substring(0, fileName.length - suffix.length)
    } else {
      "illegal"
    }
  }


}

