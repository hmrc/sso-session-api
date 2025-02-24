import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Keys.libraryDependencies
import uk.gov.hmrc.DefaultBuildSettings

val appName = "sso-session-api"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.4"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    playDefaultPort := 9551,
    libraryDependencies ++= AppDependencies(),
    scalacOptions ++= Seq(
      "-Werror",
      "-Wconf:msg=Flag.*repeatedly:s",
      "-Wconf:src=routes/.*&msg=unused import:s",
      "-Wconf:src=routes/.*&msg=unused private member:s",
      "-Wconf:src=twirl/.*&msg=unused import:s"
    ),
    scalafmtOnCompile := true
  )
  .settings(ScoverageSettings())

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
