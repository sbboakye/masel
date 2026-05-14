package com.sbboakye.masel.app.endpoints

import org.http4s.Uri
import org.http4s.dsl.io.Root

object BaseEndpoint:

  val basePath: Uri.Path = Root / "api" / "v1"
