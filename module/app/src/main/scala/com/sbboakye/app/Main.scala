package com.sbboakye.app

import cats.effect.IO
import cats.effect.{ExitCode, IOApp}

object Main extends IOApp:

  def run(args: List[String]): IO[ExitCode] = 
    IO(ExitCode.Success)
