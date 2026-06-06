package com.sbboakye.masel.core.domain

import java.util.UUID

trait SchemaScope[A]:
  extension (a: A) def uuid: UUID
  def prefix: String

object SchemaScope:
  given SchemaScope[ChallengeId] with { extension (a: ChallengeId) def uuid: UUID = a.value; def prefix = "chal" }
  given SchemaScope[SubmissionId] with { extension (a: SubmissionId) def uuid: UUID = a.value; def prefix = "sub" }
