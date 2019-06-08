package net.imadz.git.stats

import play.api.libs.json.Json

package object services {
  case class GitRepository(repositoryUrl: String, branch: String)

  object GitRepository {
    implicit val formats = Json.format[GitRepository]
  }

  case class CreateTaskReq(repositories: List[GitRepository], fromDay: String, toDay: Option[String])

  object CreateTaskReq {
    implicit val formats = Json.format[CreateTaskReq]
  }

  case class CreateTaskResp(id: Int)

  object CreateTaskResp {
    implicit val formats = Json.format[CreateTaskResp]
  }
}
