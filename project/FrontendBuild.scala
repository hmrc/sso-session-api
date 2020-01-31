import sbt._

object FrontendBuild extends Build with MicroService {
  override val appName = "sso-session-api"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._

  val compile = Seq(
    cache,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.3.0",
    "uk.gov.hmrc" %% "government-gateway-domain" % "2.5.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.48.0-play-26",
    "uk.gov.hmrc" %% "play-language" % "4.2.0-play-26"
  )

  val test = Seq(
    "org.pegdown" % "pegdown" % "1.6.0" % "test, it",
    "uk.gov.hmrc" %% "government-gateway-test" % "2.5.0-play-26" % "test,it",
    "com.github.tomakehurst" % "wiremock" % "2.15.0" % "it" exclude("org.apache.httpcomponents","httpclient") exclude("org.apache.httpcomponents","httpcore"),
    "org.mockito" % "mockito-core" % "2.16.0" % "test,it",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % "test, it"
  )

  def apply() = compile ++ test
}
