package net.imadz.git.stats.services

object CommitParser {

  trait CommitParserState {
    def read(line: String): CommitParserState

    def commits: List[CommitOnFile]
  }

  case class ReadingCommitHeaderState(acc: List[CommitOnFile], file: String) extends CommitParserState {
    override def read(line: String): CommitParserState = {
      if (line.startsWith("commit")) ReadingAuthorState(acc, line.split(" ").last, file)
      else this
    }

    override def commits: List[CommitOnFile] = acc
  }

  case class ReadingAuthorState(acc: List[CommitOnFile], commitId: String, file: String) extends CommitParserState {

    val r = """.*<(.*)@.*""".r

    def authorOf(line: String): String = line match {
      case r(name) => name
    }

    override def read(line: String): CommitParserState =
      if (line.startsWith("Author")) ReadingDateState(acc, commitId, file, authorOf(line))
      else this

    override def commits: List[CommitOnFile] = acc
  }

  case class ReadingDateState(acc: List[CommitOnFile], commitId: String, file: String, author: String) extends CommitParserState {
    val r = """Date:   (.*)""".r

    override def read(line: String): CommitParserState = line match {
      case r(dateTime) => ReadingMessageState(acc, commitId, file, author, dateTime)
    }

    override def commits: List[CommitOnFile] = acc
  }

  case class ReadingMessageState(acc: List[CommitOnFile], commitId: String, file: String, author: String, dateTime: String, message: String = "") extends CommitParserState {
    override def read(line: String): CommitParserState =
      if (line.startsWith("commit")) ReadingAuthorState(CommitOnFile(commitId, file, author, dateTime, message) :: acc, line.split(" ").last, file)
      else this.copy(message = this.message + "\n" + line)

    override def commits: List[CommitOnFile] = CommitOnFile(commitId, file, author, dateTime, message) :: acc
  }

  /*
  commit c3e12d5f2c190a6a37829f07b83fb1c29a610ea0
Author: Barry Zhong <zhongdj@gmail.com>
Date:   Wed Oct 16 16:32:41 2013 +0800

    Fix POM dependencies incorrect scope issue

    After this fix the crmp.ear file won't include unneccessary files

   */
  def read(file: String): String => List[CommitOnFile] = lines =>
    lines.split("""\n""").foldLeft[CommitParserState](ReadingCommitHeaderState(Nil, file))(_ read _).commits

}
