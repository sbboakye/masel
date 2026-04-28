import sbt.*

object Dependencies {

  object Versions {
    // Core
    val cats = "2.13.0"
    val catsEffect = "3.7.0"
    val fs2 = "3.13.0"
    val iron = "3.3.0"

    // Http API
    val http4s = "0.23.34"

    // Database
    val skunk = "1.0.0"
    val flyway = "12.4.0"
    val postgresql = "42.7.10"

    // JSON
    val circe = "0.14.15"
    val circeYaml = "1.15.0"

    // Config
    val ciris = "3.14.1"

    // Logging
    val log4cats = "2.8.0"
    val logback = "1.5.32"

    // Testing
    val munit              = "1.3.0"
    val munitCatsEffect    = "2.2.0"
    val scalaCheck         = "1.19.0"
    val testcontainers     = "0.44.1"
  }

  // Core
  val cats = "org.typelevel" %% "cats-core" % Versions.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
  val fs2Core = "co.fs2" %% "fs2-core" % Versions.fs2
  val fs2IO = "co.fs2" %% "fs2-io" % Versions.fs2
  val ironCore = "io.github.iltotore" %% "iron" % Versions.iron
  val ironCirce = "io.github.iltotore" %% "iron-circe" % Versions.iron
  val ironCiris = "io.github.iltotore" %% "iron-ciris" % Versions.iron
  val ironCats = "io.github.iltotore" %% "iron-cats" % Versions.iron

  // HTTP & API
  val http4sEmberServer = "org.http4s" %% "http4s-ember-server" % Versions.http4s
  val http4sEmberClient = "org.http4s" %% "http4s-ember-client" % Versions.http4s
  val http4sCirce       = "org.http4s" %% "http4s-circe"        % Versions.http4s
  val http4sDsl         = "org.http4s" %% "http4s-dsl"          % Versions.http4s

  // Database
  val skunkCore = "org.tpolecat" %% "skunk-core" % Versions.skunk
  val skunkCirce = "org.tpolecat" %% "skunk-circe" % Versions.skunk
  val flyway              = "org.flywaydb"  % "flyway-core"           % Versions.flyway
  val flywayPostgres      = "org.flywaydb"  % "flyway-database-postgresql" % Versions.flyway
  val postgresql          = "org.postgresql" % "postgresql"            % Versions.postgresql

  // JSON
  val circeCore = "io.circe" %% "circe-core" % Versions.circe
  val circeGeneric = "io.circe" %% "circe-generic" % Versions.circe
  val circeParser = "io.circe" %% "circe-parser" % Versions.circe
  val circeYaml = "io.circe" %% "circe-yaml" % Versions.circeYaml

  // Config
  val ciris = "is.cir" %% "ciris" % Versions.ciris
  val cirisCirce = "is.cir" %% "ciris-circe" % Versions.ciris
  val cirisEnumeratum = "is.cir" %% "ciris-enumeratum" % Versions.ciris
  val cirisHttp4s = "is.cir" %% "ciris-http4s" % Versions.ciris

  // Logging
  val log4catsSlf4j = "org.typelevel" %% "log4cats-slf4j" % Versions.log4cats
  val logback = "ch.qos.logback" % "logback-classic" % Versions.logback

  // Testing
  val munit = "org.scalameta" %% "munit" % Versions.munit % Test
  val munitCatsEffect = "org.typelevel"  %% "munit-cats-effect"   % Versions.munitCatsEffect % Test
  val scalaCheck = "org.scalacheck" %% "scalacheck" % Versions.scalaCheck % Test
  val testcontainersPostgres = "com.dimafeng" %% "testcontainers-scala-postgresql" % Versions.testcontainers % Test
  val testcontainersMunit    = "com.dimafeng" %% "testcontainers-scala-munit"      % Versions.testcontainers % Test

  // Dependency groups
  val coreDeps: Seq[ModuleID] = Seq(cats, catsEffect, fs2Core, fs2IO, circeCore, circeGeneric, ironCore, ironCirce, ironCiris, ironCats)

  val persistenceDeps: Seq[ModuleID] = Seq(cats, catsEffect, skunkCore, skunkCirce, postgresql, flyway, flywayPostgres, log4catsSlf4j)

  val apiDeps: Seq[ModuleID] = Seq(http4sEmberServer, http4sEmberClient, http4sCirce, http4sDsl)

  val engineDeps: Seq[ModuleID] = Seq(fs2Core, catsEffect, log4catsSlf4j)

  val appDeps: Seq[ModuleID] = Seq(
    circeCore, circeGeneric, circeParser, circeYaml, ciris, cirisCirce, cirisEnumeratum,
    cirisHttp4s, ironCiris, ironCats, logback,
    http4sEmberServer, http4sEmberClient, http4sCirce, http4sDsl
  )

  val testDeps: Seq[ModuleID] = Seq(munit, munitCatsEffect, scalaCheck, testcontainersMunit)

}
