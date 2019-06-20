package net.imadz.git.stats.services

import java.io.File

import com.google.inject.Inject
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

  def exec(dir: String, profile: Option[String]): Either[AppError, Future[String]] = {
    val files: Either[AppError, List[String]] = lsTreeObjects(dir)
    val commits: Either[AppError, List[CommitOnFile]] = files.flatMap(fs => sequence(fs.map(readCommits(dir)))).map(_.flatten)
    val taggedCommits: Either[AppError, List[TaggedCommit]] = commits.map(cs => cs.flatMap(tagging(profile)))
    commits
      .map(batchInsertOrUpdate(dir))
      .flatMap(_ => taggedCommits.map(batchInsertOrUpdateTaggedCommit(dir)))
      .map(_.map(_.mkString(",")))
  }

  private def lsTreeObjects(dir: String): Either[AppError, List[String]] = try {
    println(s"/opt/docker/git-ls-tree.sh $dir")
    Right(s"/opt/docker/git-ls-tree.sh $dir" !!)
      .map(_.split("""\n""").toList)
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
    dbConfig.db
      .run(Tables.GitCommit ++= commits)
      .map(_ => xs)
  }

  private def batchInsertOrUpdateTaggedCommit(dir: String)(xs: List[TaggedCommit]): Future[List[TaggedCommit]] = {
    val commits = xs.map(x => Tables.TaggedCommitRow(0, dateOf(x.commit.date), x.tag, dir, x.commit.id, x.commit.file))
    dbConfig.db.run(Tables.TaggedCommit ++= commits)
      .map(_ => xs)
  }

  private val javaOnionTagger: Tagger = taggerOf("conf/tags/java.onion.profile.properties")
  val builtInTaggers: Map[String, Tagger] = Map(
    "java.onion" -> javaOnionTagger,
    "golang" -> taggerOf("conf/tags/golang.profile.properties"),
  )

  lazy val customTaggers: Map[String, Tagger] = new File("/root/tags")
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

  lazy val allTaggers = (builtInTaggers ++ customTaggers).withDefaultValue(javaOnionTagger)

  private def srcMain: CommitOnFile => Boolean = commit =>
    commit.file.contains("src/main") || commit.file.contains("app/")

  private def infrastructure: CommitOnFile => Option[String] = commit =>
    if (commit.file.contains("infrastructure/")) Some("infrastructure") else None

  private def application: CommitOnFile => Option[String] = commit =>
    if (commit.file.contains("application/")) Some("application") else None

  private def domain: CommitOnFile => Option[String] = commit =>
    if (commit.file.contains("domain/") || commit.file.contains("app/db") || commit.file.contains("app/service/async/jobs")) Some("domain") else None

  private def controller: CommitOnFile => Option[String] = commit => Some("controller").filter(commit.file.contains)

  private def gateway: CommitOnFile => Option[String] = commit =>
    if (commit.file.contains("gateway") || commit.file.contains("app/service/remote")) Some("gateway") else None

  private def configuration: CommitOnFile => Option[String] = commit =>
    if (commit.file.contains("configuration") || commit.file.contains("app/configs")) Some("configuration") else None

  private def tags(profile: Option[String]): CommitOnFile => List[String] = commit =>
    allTaggers(profile.getOrElse("java.onion"))(commit.file)

  private def tags2(profile: Option[String]): CommitOnFile => List[String] = commit =>
    List(infrastructure, application, domain, controller, gateway, configuration)
      .map(f => (commit: CommitOnFile) => if (!srcMain(commit)) None else f(commit))
      .map(f => f(commit))
      .filter(_.nonEmpty)
      .map(_.getOrElse(""))
}

object Demo extends App with Tagged {
  private val javaOnionFile = new File("/Users/barry/Workspaces/external/git-stats-backend/conf/tags/java.onion.profile.properties")
  //  val tag1 = taggerOf("conf/tags/java.onion.profile.properties")
  val tag1 = taggerOf(javaOnionFile)
  val tag2 = taggerOf("conf/tags/golang.profile.properties")
  val tag = compose(tag1, tag2)
  println(tag("/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home/bin/java"))
  println(tag("./src/main/java/com/xiaoju/ep/cooper/pegasus/infrastructure/gateway/IdStartupRunner.java"))
  println(tag("app/service/remote/fragment/fragment.go"))

  private def profileName(fileName: String): String = {
    fileName.substring(0, fileName.length - ".profile.properties".length)
  }

  private def profileName(file: File): String = {
    file.getName.substring(0, file.getName.length - ".profile.properties".length)
  }

  println(profileName("golang.profile.properties"))
  println(profileName(javaOnionFile))
}