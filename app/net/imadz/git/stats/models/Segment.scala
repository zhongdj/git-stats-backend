package net.imadz.git.stats.models

case class Metric(day: String, project: String, developer: String, metric: String, value: String) {
  override def toString: String = s"""$day, $project, $developer, $metric, $value"""
}

case class Segment(day: String, project: String, fields: Array[String] = Array(), metrics: List[Metric] = Nil)

object SegmentParser {

  private def toMetric(day: String, project: String, developer: String): (String, String) => Metric =
    (value: String, metricName: String) => Metric(day, project, developer, metricName, value)

  private def metricsOf(line: String, fields: Array[String], day: String, project: String): List[Metric] = {
    val r = """(.*),: (\d+) files changed, (\d+) insertions\(\+\), (\d+) deletions\(\-\), (\-*\d+) net""".r

    line match {
      case r(maybeMoreNames, files, inserts, deletes, nets) =>
        val name = maybeMoreNames.split(",").last
        List(
          Metric(day, project, name, "files", files),
          Metric(day, project, name, "inserts", inserts),
          Metric(day, project, name, "deletes", deletes),
          Metric(day, project, name, "net", nets),
        )
      case _ =>
        println("---not matched---" + line)
        Nil
    }
  }

  trait ParserState {
    def read(line: String): ParserState

    def segments(): List[Segment]
  }

  case class ReadingSegmentHeaderState(acc: List[Segment]) extends ParserState {
    override def read(line: String): ParserState = {
      if (segmentHeader(line)) ReadingValueState(acc, Segment(dayOf(line), projectOf(line)))
      else this
    }

    override def segments(): List[Segment] = acc
  }

  case class ReadingValueState(acc: List[Segment], fieldsSegment: Segment) extends ParserState {

    def lineSpliter(line: String): Boolean = line.isEmpty

    override def read(line: String): ParserState = {
      if (line.startsWith(":")) ReadingSegmentHeaderState(acc)
      else if (lineSpliter(line)) ReadingSegmentHeaderState(fieldsSegment :: acc)
      else this.copy(fieldsSegment = Segment(fieldsSegment.day,
        fieldsSegment.project,
        Array("files", "insertions", "deletions", "net"),
        metricsOf(line, fieldsSegment.fields, fieldsSegment.day, fieldsSegment.project) ::: fieldsSegment.metrics))
    }

    override def segments(): List[Segment] = fieldsSegment :: acc
  }

  private def segmentHeader(line: String): Boolean = {
    val headers = line.split(" ")
    headers.length == 3 && headers(1) == headers(2)
  }

  private def dayOf(line: String): String = {
    line.split(" ")(1)
  }

  private def projectOf(line: String): String = {
    line.split(" ")(0)
  }

  private def fieldsOf(line: String): Array[String] = {
    line.split(",").drop(1)
  }

  def parse(stats: List[String]): List[Metric] =
    stats.foldLeft[ParserState](ReadingSegmentHeaderState(Nil))(_ read _).segments().flatMap(_.metrics)
}
