package net.imadz.git.stats.services

import org.scalatest.Matchers._
import org.scalatest.{ MustMatchers, WordSpecLike }

class TaggedTest extends WordSpecLike with Tagged with MustMatchers {

  "Tagger" must {
    "generate two tags with two matches" in {
      val tagger = taggerOf("conf/tags/java.onion.profile.properties")
      val tags = tagger("./src/main/iron/man/infrastructure/gateway/IdStartupRunner.java")
      tags should be(List("infrastructure", "gateway"))
    }
    "generate no tags with no matches" in {
      val tagger = taggerOf("conf/tags/java.onion.profile.properties")
      val tags = tagger("./src/main/iron/man/infrastructur/gatewa/IdStartupRunner.java")
      tags should be(Nil)
    }
  }

}
