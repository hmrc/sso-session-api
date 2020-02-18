import sbt._

private object AppDependencies {

  import play.sbt.PlayImport._

  val compile = Seq(
    ehcache,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.3.0",
    "uk.gov.hmrc" %% "government-gateway-domain" % "2.7.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.48.0-play-26"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "government-gateway-test" % "2.6.0" % "test,it"
  )

  def apply() = compile ++ test
}
