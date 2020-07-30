import sbt._

private object AppDependencies {

  import play.sbt.PlayImport._

  val compile = Seq(
    ehcache,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.14.0",
    "uk.gov.hmrc" %% "government-gateway-domain" % "5.1.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.52.0-play-26"
  )

  val test = Seq(
    "uk.gov.hmrc" %% "government-gateway-test" % "3.2.0" % "test,it"
  )

  def apply() = compile ++ test
}
