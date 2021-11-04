import sbt._

private object AppDependencies {

  import play.sbt.PlayImport._

  val compile = Seq(
    ehcache,
    // claims to be a frontend even though it's definitely a backend so it has access to `sso.encryption.key`,
    // and because it appears to need to set cookies
    "uk.gov.hmrc" %% "bootstrap-frontend-play-28" % "5.16.0",
    "uk.gov.hmrc" %% "government-gateway-domain" % "7.0.0-play-28"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "government-gateway-test" % "4.6.0-play-28" % "test,it"
  )

  def apply() = compile ++ test
}
