package net.imadz.git.stats.models

import java.io.File

import net.imadz.git.stats.services.Tagged
import org.scalatest.Matchers._
import org.scalatest.{MustMatchers, WordSpecLike}

class InvertedFileIndexTest extends WordSpecLike with Tagged with MustMatchers {

  "InvertedFileIndex" must {
    "top level file should be found from index" in {
      //Given
      val topLevelFile = "genericfunc.go:43:1"
      val rootPath = "/root/.tasks/1/go-linq"
      val givenFile = "/root/.tasks/1/go-linq/genericfunc.go"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search(topLevelFile.split(":")(0))
      //Then
      maybeFile should be(Some(new File(givenFile)))
    }

    "un-existing top level file should not be found from index" in {
      //Given
      val topLevelFile = "genericfunc.go:43:1"
      val rootPath = "/root/.tasks/1/go-linq"
      val givenFile = "/root/.tasks/1/go-linq/genericfunc.go1"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search(topLevelFile.split(":")(0))
      //Then
      maybeFile should be(None)
    }

    "middle level file should be found from index" in {
      //Given
      val secondLevelFile = "/1st-level/genericfunc.go:43:1"
      val rootPath = "/root/.tasks/1/go-linq"
      val givenFile = "/root/.tasks/1/go-linq/1st-level/genericfunc.go"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search("1st-level/genericfunc.go")
      //Then
      maybeFile should be(Some(new File(givenFile)))
    }
    "deep middle level file should be found from index" in {
      //Given
      val secondLevelFile = "/1st-level/2nd-level/genericfunc.go:43:1"
      val rootPath = "/root/.tasks/1/go-linq"
      val givenFile = "/root/.tasks/1/go-linq/1st-level/2nd-level/genericfunc.go"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search("1st-level/2nd-level/genericfunc.go")
      //Then
      maybeFile should be(Some(new File(givenFile)))
    }

    "abbreviated path should be found from index" in {
      //Given
      val secondLevelFile = "...l/a_very_very_long_file_name.go:183:1"
      val rootPath = "/root/.tasks/1/go-linq"
      val givenFile = "/root/.tasks/1/go-linq/1st-level/a_very_very_long_file_name.go"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search("...l/a_very_very_long_file_name.go")
      //Then
      maybeFile should be(Some(new File(givenFile)))

    }

    "deep abbreviated path should be found from index" in {
      //Given
      val secondLevelFile = "...l/a_very_very_long_file_name.go:183:1"
      val rootPath = "/root/.tasks/1/go-linq"
      val givenFile = "/root/.tasks/1/go-linq/1st-level/2nd-level/a_very_very_long_file_name.go"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search("...l/2nd-level/a_very_very_long_file_name.go")
      //Then
      maybeFile should be(Some(new File(givenFile)))

    }

    "deep abbreviated path should be found from index with /" in {
      //Given
      val secondLevelFile = "...l/a_very_very_long_file_name.go:183:1"
      val rootPath = "/"
      val givenFile = "/biz/handler/checker/talent_custom_field_schema.go"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search("talent_custom_field_schema.go")
      //Then
      maybeFile should be(Some(new File(givenFile)))

    }
    "1st level abbreviated file should be found from index" in {
      //Given
      val secondLevelFile = "...a_very_very_long_file_name.go:183:1"
      val rootPath = "/"
      val givenFile = "/biz/handler/checker/talent_custom_field_schema_a_very_very_long_file_name.go"
      val givenIndex = InvertedFileIndex(rootPath) + givenFile
      //When
      val maybeFile = givenIndex.search("...a_very_very_long_file_name.go")
      //Then
      maybeFile should be(Some(new File(givenFile)))
    }

  }


}
