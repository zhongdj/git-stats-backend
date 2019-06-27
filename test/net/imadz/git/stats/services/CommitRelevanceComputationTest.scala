package net.imadz.git.stats.services

import org.scalatest.Matchers._
import org.scalatest.{MustMatchers, WordSpecLike}

class CommitRelevanceComputationTest extends WordSpecLike with MustMatchers with CommitRelevanceComputation {

  "SymmetryMatrix" must {
    "read (1, 2, 3) and (2, 1, 3) after storing (1, 2, 3) " in {
      val matrix = SymmetryMatrix()
        .increase(1, 2)
        .increase(1, 2)
        .increase(1, 2)

      matrix.value(1, 2) should be(3)
      matrix.value(2, 1) should be(3)
    }

    "read (1, 1, 0) while storing nothing" in {
      SymmetryMatrix().value(1, 1) should be(0)
    }
  }

  "Categorize Algorithm" must {
    "create a category with a single commit" in {
    }

    "create two category with two irrelevant commits" in {
    }

  }
}
