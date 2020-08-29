package net.imadz.git.stats.services

object GolangFuncsParser extends App {
  var lines =
    """(*articleUsecase) fillAuthorDetails article/usecase/article_ucase.go:33:1,lines=31.0,in_params=2.0,complexity=10.0,complexity/lines=0.3
    |main app/main.go:33:1,lines=31.0,in_params=0.0,complexity=4.0,complexity/lines=0.1
    |(*mysqlArticleRepository) fetch ...ository/mysql/mysql_article.go:23:1,lines=29.0,in_params=3.0,complexity=5.0,complexity/lines=0.2
    |(*ArticleRepository) Fetch domain/mocks/ArticleRepository.go:28:1,lines=18.0,in_params=3.0,complexity=5.0,complexity/lines=0.3
    |(*ArticleUsecase) Fetch domain/mocks/ArticleUsecase.go:28:1,lines=18.0,in_params=3.0,complexity=5.0,complexity/lines=0.3
    |(*mysqlArticleRepository) Update ...sitory/mysql/mysql_article.go:162:1,lines=15.0,in_params=2.0,complexity=5.0,complexity/lines=0.3
    |(*mysqlArticleRepository) Delete ...sitory/mysql/mysql_article.go:137:1,lines=15.0,in_params=2.0,complexity=5.0,complexity/lines=0.3
    |(*mysqlAuthorRepo) getOne ...tory/mysql/mysql_repository.go:21:1,lines=13.0,in_params=3.0,complexity=2.0,complexity/lines=0.2
    |(*ArticleHandler) Store ...livery/http/article_handler.go:79:1,lines=13.0,in_params=1.0,complexity=4.0,complexity/lines=0.3
    |(*mysqlArticleRepository) Store ...sitory/mysql/mysql_article.go:118:1,lines=13.0,in_params=2.0,complexity=4.0,complexity/lines=0.3
    |getStatusCode ...ivery/http/article_handler.go:118:1,lines=13.0,in_params=1.0,complexity=6.0,complexity/lines=0.5
    |(*mysqlArticleRepository) Fetch ...ository/mysql/mysql_article.go:63:1,lines=12.0,in_params=3.0,complexity=5.0,complexity/lines=0.4
    |(*articleUsecase) Fetch article/usecase/article_ucase.go:84:1,lines=12.0,in_params=3.0,complexity=4.0,complexity/lines=0.3
    |(*mysqlArticleRepository) GetByTitle ...sitory/mysql/mysql_article.go:101:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*ArticleUsecase) GetByTitle domain/mocks/ArticleUsecase.go:79:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*AuthorRepository) GetByID domain/mocks/AuthorRepository.go:14:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*mysqlArticleRepository) GetByID ...ository/mysql/mysql_article.go:83:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*ArticleUsecase) GetByID domain/mocks/ArticleUsecase.go:58:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*articleUsecase) GetByID article/usecase/article_ucase.go:104:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*ArticleRepository) GetByTitle domain/mocks/ArticleRepository.go:79:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*ArticleRepository) GetByID domain/mocks/ArticleRepository.go:58:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*articleUsecase) GetByTitle article/usecase/article_ucase.go:129:1,lines=11.0,in_params=2.0,complexity=3.0,complexity/lines=0.3
    |(*ArticleHandler) FetchArticle ...livery/http/article_handler.go:36:1,lines=10.0,in_params=1.0,complexity=2.0,complexity/lines=0.2
    |(*ArticleHandler) GetByID ...livery/http/article_handler.go:52:1,lines=10.0,in_params=1.0,complexity=3.0,complexity/lines=0.3
    |(*ArticleHandler) Delete ...ivery/http/article_handler.go:101:1,lines=10.0,in_params=1.0,complexity=3.0,complexity/lines=0.3""".stripMargin

  parseFunctionMetrics(lines).foreach(println)
  case class FuncMetric(name: String, abbrPath: String, lines: Int, params: Int, complexity: Int, complexityRate: Double)

  private def toFunctionMetrics: String => List[FuncMetric] = line => {
    val vars = line.split(",")
    try {
      List(
        FuncMetric(func(vars(0)), path(vars(0)), intValue(vars(1)), intValue(vars(2)), intValue(vars(3)), doubleValue(vars(4)))
      )
    } catch {
      case e: Throwable =>
        println(line)
        e.printStackTrace()
        Nil
    }
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
      .map(_.trim)
      .filter(_.nonEmpty)
      .flatMap(toFunctionMetrics)
      .toList
  }
}
