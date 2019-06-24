package net.imadz.git.stats.services

import org.scalatest.Matchers._
import org.scalatest.{MustMatchers, WordSpecLike}

class CommitRelevanceComputationTest extends WordSpecLike with MustMatchers with CommitRelevanceComputation {

  val file1 = "app/service/async/jobs/publish.go"
  val file2 = "app/service/async/jobs/publish_test.go"
  val file3 = "app/service/remote/fragment/client.go"
  val file4 = "app/service/remote/gemini/gemini.go"
  val file5 = "app/config/config.go"
  val commitId = "0120a1aa995aa6bcbf626bc3a829fd30e7c3f028"

  case class Obj(key: String, id: String) extends Keyed

  "Categorize Algorithm" must {
    "create a category with a single commit" in {
      //given
      val file1 = "app/service/async/jobs/publish.go"
      val file2 = "app/service/async/jobs/publish_test.go"
      val file3 = "app/service/remote/fragment/client.go"
      val file4 = "app/service/remote/gemini/gemini.go"
      val file5 = "app/config/config.go"
      val commitId = "0120a1aa995aa6bcbf626bc3a829fd30e7c3f028"

      val aGitCommit = List(
        Obj(commitId, file1),
        Obj(commitId, file2),
        Obj(commitId, file3),
        Obj(commitId, file4)
      )
      //when
      val categories = categorize(aGitCommit)
      //then
      categories should be(List(Set(
        Classified(Obj(commitId, file1), 1),
        Classified(Obj(commitId, file2), 1),
        Classified(Obj(commitId, file3), 1),
        Classified(Obj(commitId, file4), 1)
      )))
    }
  }
}
