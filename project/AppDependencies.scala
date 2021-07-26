import sbt._

private object AppDependencies {

  import play.sbt.PlayImport._

  val compile = Seq(
    ehcache,
    // claims to be a frontend even though it's definitely a backend so it has access to `sso.encryption.key`,
    // and because it appears to need to set cookies
    "uk.gov.hmrc" %% "bootstrap-frontend-play-27" % "5.7.0",
    "uk.gov.hmrc" %% "government-gateway-domain" % "6.4.0-play-27",
    "uk.gov.hmrc" %% "govuk-template" % "5.69.0-play-27"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "government-gateway-test" % "4.4.0-play-27" % "test,it"
  )

  def apply() = compile ++ test
}
