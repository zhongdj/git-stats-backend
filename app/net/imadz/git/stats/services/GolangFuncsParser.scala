package net.imadz.git.stats.services

object GolangFuncsParser extends App {
  var lines =
    """
      |(*varCounterVisitor) Visit ...ongfuncs/internal/variables.go:28:1,lines=36.0,in_params=1.0,complexity=21.0,complexity/lines=0.6
      |main ...gfuncs/cmd/golongfuncs/main.go:13:1,lines=32.0,in_params=0.0,complexity=6.0,complexity/lines=0.2
      |(*Visitor) Visit ...olongfuncs/internal/runner.go:141:1,lines=26.0,in_params=1.0,complexity=9.0,complexity/lines=0.3
      |printStats ...gfuncs/cmd/golongfuncs/main.go:80:1,lines=18.0,in_params=2.0,complexity=8.0,complexity/lines=0.4
      |calculateLines ...a/golongfuncs/internal/body.go:36:1,lines=18.0,in_params=6.0,complexity=4.0,complexity/lines=0.2
      |analyzeFile ...golongfuncs/internal/runner.go:42:1,lines=17.0,in_params=2.0,complexity=7.0,complexity/lines=0.4
      |(FunctionStats) Get ...golongfuncs/internal/models.go:51:1,lines=15.0,in_params=1.0,complexity=7.0,complexity/lines=0.5
      |(*complexityVisitor) Visit ...ngfuncs/internal/complexity.go:20:1,lines=15.0,in_params=1.0,complexity=8.0,complexity/lines=0.5
      |NewVisitor ...olongfuncs/internal/runner.go:117:1,lines=15.0,in_params=4.0,complexity=3.0,complexity/lines=0.2
      |prepareParams ...gfuncs/cmd/golongfuncs/main.go:58:1,lines=15.0,in_params=4.0,complexity=6.0,complexity/lines=0.4
      |Do ...golongfuncs/internal/runner.go:21:1,lines=14.0,in_params=2.0,complexity=4.0,complexity/lines=0.3
      |buildStats ....com/fzipp/gocyclo/gocyclo.go:164:1,lines=13.0,in_params=3.0,complexity=3.0,complexity/lines=0.2
      |analyzeDirRecursively ...golongfuncs/internal/runner.go:88:1,lines=13.0,in_params=5.0,complexity=7.0,complexity/lines=0.5
      |analyzeFile ....com/fzipp/gocyclo/gocyclo.go:101:1,lines=12.0,in_params=2.0,complexity=2.0,complexity/lines=0.2
      |analyzeDir ....com/fzipp/gocyclo/gocyclo.go:110:1,lines=11.0,in_params=5.0,complexity=4.0,complexity/lines=0.4
      |countLines ...a/golongfuncs/internal/body.go:87:1,lines=11.0,in_params=2.0,complexity=4.0,complexity/lines=0.4
      |countTodos ...a/golongfuncs/internal/body.go:70:1,lines=11.0,in_params=2.0,complexity=5.0,complexity/lines=0.5
      |ParseTypes ...a/golongfuncs/internal/body.go:20:1,lines=10.0,in_params=1.0,complexity=3.0,complexity/lines=0.3
      |(*complexityVisitor) Visit ....com/fzipp/gocyclo/gocyclo.go:215:1,lines=10.0,in_params=1.0,complexity=5.0,complexity/lines=0.5
      |""".stripMargin

  case class FuncMetric(name: String, abbrPath: String, lines: Int, params: Int, complexity: Int, complexityRate: Double)

  private def toFunctionMetrics: String => FuncMetric = line => {
    val vars = line.split(",")
    FuncMetric(func(vars(0)), path(vars(0)), intValue(vars(1)), intValue(vars(2)), intValue(vars(3)), doubleValue(vars(4)))
  }

  private def func(e: String): String = {
    val r = e.split(" ")
    if (r.length >= 3) s"${r(0)} ${r(1)}"
    else r(0)
  }

  private def path(e: String): String = {
    val r = e.split(" ")
    if (r.length >= 3) s"${r(2)}"
    else r(1)
  }

  private def intValue(e: String): Int = e.split("=")(1).toDouble.toInt

  private def doubleValue(e: String): Double = e.split("=")(1).toDouble

  def parseFunctionMetrics(lines: String): List[FuncMetric] = {
    lines.split("\n")
      .filter(_.nonEmpty)
      .map(toFunctionMetrics)
      .toList
  }
}
