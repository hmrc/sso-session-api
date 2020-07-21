import TestPhases.{TemplateItTest, TemplateTest}
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.ExternalService
import uk.gov.hmrc.ServiceManagerPlugin.Keys.itDependenciesList
import uk.gov.hmrc.ServiceManagerPlugin.serviceManagerSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

lazy val externalServices = List(
  ExternalService(name = "SSO"),
  ExternalService(name = "SSO_FRONTEND"),
  ExternalService(name = "OPENID_CONNECT_IDTOKEN"),
  ExternalService(name = "AUTH"),
  ExternalService(name = "AUTH_LOGIN_API"),
  ExternalService(name = "USER_DETAILS"),
  ExternalService(name = "IDENTITY_VERIFICATION")
)

lazy val microservice = Project("sso-session-api", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory)
  .settings(majorVersion := 0)
  .settings(scalaSettings: _*)
  .settings(scalaVersion := "2.12.11")
  .settings(scalacOptions ++= Seq("-Xfatal-warnings", "-feature"))
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(libraryDependencies ++= AppDependencies())
  .settings(Repositories.playPublishingSettings: _*)
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .configs(IntegrationTest)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .settings(serviceManagerSettings: _*)
  .settings(itDependenciesList := externalServices)
  .settings(
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
  )
  .settings(ScalariformSettings())
  .settings(ScoverageSettings())
  .settings(SilencerSettings())
  .settings(PlayKeys.playDefaultPort := 9237)