package net.imadz.git.stats.services

import org.scalatest.Matchers._
import org.scalatest.{MustMatchers, WordSpecLike}

class RelevanceComputationTest extends WordSpecLike with MustMatchers with RelevanceComputation {

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

  "count" must {
    "count Nil as Empty" in {
      count(Nil) should be(SymmetryMatrix())
    }

    "count List(List(1)) as 1 => 1 => 1" in {
      count(List(List(1))).value(1, 1) should be(1)
    }

    "count List(List(1, 2)) as 1 => 1 => 1 and 2 => 2 => 1 and 1 => 2 => 1" in {
      val matrix = count(List(List(1, 2)))
      matrix.value(1, 1) should be(1)
      matrix.value(2, 2) should be(1)
      matrix.value(1, 2) should be(1)
      matrix.value(2, 1) should be(1)
    }

    "count List(List(1, 2), List(1, 2)) as 1 => 1 => 2, 2 => 2 => 2, and 1 => 2 => 2" in {
      val matrix = count(List(List(1, 2), List(1, 2)))
      matrix.value(1, 1) should be(2)
      matrix.value(2, 2) should be(2)
      matrix.value(1, 2) should be(2)
      matrix.value(2, 1) should be(2)
    }
    "count List(List(1, 2), List(2, 3)) as 1 => 1 => 1, 2 => 2 => 2, 2 => 3 => 1, and 1 => 3 => 0" in {
      val matrix = count(List(List(1, 2), List(2, 3)))
      matrix.value(1, 1) should be(1)
      matrix.value(2, 2) should be(2)
      matrix.value(1, 2) should be(1)
      matrix.value(2, 3) should be(1)
    }

    "count does not stack overflow within 1000 elements" in {
      count(List(1 to 10000 toList))
    }
  }

  "score" must {
    "get 1 => 1 => 1 with 1 total" in {
      score(1)(SymmetryMatrix(Map(1 -> Map(1 -> 1))))(1)(1) should be(1.toDouble)
    }
    "get 1 => 1 => 0.5, 2 => 2 => 0.5 and 1 => 2 => 0 with 2 total" in {
      val matrix = SymmetryMatrix(Map(1 -> Map(1 -> 1), 2 -> Map(2 -> 1)))
      score(2)(matrix)(1)(1) should be(0.5D)
      score(2)(matrix)(2)(2) should be(0.5D)
      score(2)(matrix)(2)(1) should be(0D)
    }
  }

  "Categorize Algorithm" must {
    "create a category with a single commit" in {
    }

    "create two category with two irrelevant commits" in {
    }

  }
}
