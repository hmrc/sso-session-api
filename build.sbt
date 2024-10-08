import uk.gov.hmrc.DefaultBuildSettings

val appName = "sso-session-api"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.5.0"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(DefaultBuildSettings.scalaSettings *)
  .settings(DefaultBuildSettings.defaultSettings() *)
  .settings(
    scalacOptions += "-Wconf:msg=unused import&src=html/.*:s",
    scalacOptions += "-Wconf:msg=unused import&src=routes/.*:s",
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(libraryDependencies ++= AppDependencies())
  .settings(ScoverageSettings())
  .settings(PlayKeys.playDefaultPort := 9551)
  .settings(scalafmtOnCompile := true)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
