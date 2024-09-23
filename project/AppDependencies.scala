import sbt.*

private object AppDependencies {

  import play.sbt.PlayImport.*

  private val bootstrapVersion = "9.4.0"

  val compile: Seq[ModuleID] = Seq(
    ehcache,
    // claims to be a frontend even though it's definitely a backend so it has access to `sso.encryption.key`,
    // and because it appears to need to set cookies
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % bootstrapVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion % Test,
    "org.scalatestplus"      %% "mockito-4-11"           % "3.2.17.0"       % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
