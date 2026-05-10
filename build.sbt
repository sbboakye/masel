ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.3"
ThisBuild / organization := "com.sbboakye"

lazy val core = (project in file("module/core"))
  .settings(
    name := "masel-core",
    libraryDependencies ++= Dependencies.coreDeps ++ Dependencies.testDeps,
  )

lazy val engine = (project in file("module/engine"))
  .dependsOn(core)
  .settings(
    name := "masel-engine",
    libraryDependencies ++= Dependencies.engineDeps ++ Dependencies.testDeps,
  )

lazy val persistence = (project in file("module/persistence"))
  .dependsOn(core)
  .settings(
    name := "masel-persistence",
    Test / unmanagedResourceDirectories += baseDirectory.value / "src/main/resources",
    libraryDependencies ++= Dependencies.persistenceDeps ++ Dependencies.testDeps ++ Seq(
      Dependencies.testcontainersPostgres,
    ),
  )

lazy val app = (project in file("module/app"))
  .dependsOn(
    core,
    engine,
    persistence,
  )
  .settings(
    name := "masel-app",
    libraryDependencies ++= Dependencies.appDeps ++ Dependencies.apiDeps ++ Dependencies.testDeps,
  )

lazy val root = (project in file("."))
  .settings(
    name := "masel",
  )
  .aggregate(core, engine, persistence, app)
