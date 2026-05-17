package com.sbboakye.masel.app.routes

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.sbboakye.masel.app.endpoints.{HealthResponse, ReadinessResponse}
import com.sbboakye.masel.app.services.InMemoryAppDb
import com.sbboakye.masel.core.ports.{AppDb, Repos}
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.implicits.*
import org.http4s.{Method, Request, Status}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

class HeartbeatRoutesTests extends AsyncFreeSpec with AsyncIOSpec with Matchers:

  private def routesFor(db: AppDb[IO]) = new HeartbeatRoutes[IO](db).routes

  "GET /health" - {
    "responds 200 with status=ok regardless of database state" in
      InMemoryAppDb.make[IO].flatMap { db =>
        val req = Request[IO](Method.GET, uri"/health")
        for {
          resp <- routesFor(db).orNotFound.run(req)
          body <- resp.as[HealthResponse]
        } yield {
          resp.status shouldBe Status.Ok
          body shouldBe HealthResponse("ok")
        }
      }
  }

  "GET /ready" - {
    "responds 200 ok when AppDb.isReady returns true" in
      InMemoryAppDb.make[IO].flatMap { db =>
        val req = Request[IO](Method.GET, uri"/ready")
        for {
          resp <- routesFor(db).orNotFound.run(req)
          body <- resp.as[ReadinessResponse]
        } yield {
          resp.status shouldBe Status.Ok
          body shouldBe ReadinessResponse("ok")
        }
      }

    "responds 503 not ready when AppDb.isReady returns false" in {
      val notReadyDb: AppDb[IO] = new AppDb[IO] {
        override def withSession[A](use: Repos[IO] => IO[A]): IO[A] =
          IO.raiseError(new IllegalStateException("should not be called"))
        override def withTransaction[A](use: Repos[IO] => IO[A]): IO[A] =
          IO.raiseError(new IllegalStateException("should not be called"))
        override def isReady: IO[Boolean] = IO.pure(false)
      }

      val req = Request[IO](Method.GET, uri"/ready")
      for {
        resp <- routesFor(notReadyDb).orNotFound.run(req)
        body <- resp.as[ReadinessResponse]
      } yield {
        resp.status shouldBe Status.ServiceUnavailable
        body shouldBe ReadinessResponse("not ready")
      }
    }
  }

  "unknown paths" - {
    "fall through (Routes.run yields None) so they don't shadow other routers" in
      InMemoryAppDb.make[IO].flatMap { db =>
        val req = Request[IO](Method.GET, uri"/totally-unknown")
        routesFor(db).run(req).value.asserting(_ shouldBe None)
      }
  }
