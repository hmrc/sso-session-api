import sbt._

object FrontendBuild extends Build with MicroService {

  import scala.util.Properties.envOrElse

  override val appName = "sso-session-api"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.sbt.PlayImport._

  val compile = Seq(
    cache,
    "uk.gov.hmrc" %% "frontend-bootstrap" % "12.9.0",
    "uk.gov.hmrc" %% "government-gateway-domain" % "1.33.0"
  )

  val test = Seq(
    "org.pegdown" % "pegdown" % "1.6.0" % "test, it",
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % "test, it",
    "uk.gov.hmrc" %% "government-gateway-test" % "1.8.0" % "it",
    "com.github.tomakehurst" % "wiremock" % "2.15.0" % "it" exclude("org.apache.httpcomponents","httpclient") exclude("org.apache.httpcomponents","httpcore"),
    "org.mockito" % "mockito-core" % "2.16.0" % "test,it",
    "org.scalatest" %% "scalatest" % "3.0.0" % "test",
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1" % "test, it"
  )

  def apply() = compile ++ test
}
