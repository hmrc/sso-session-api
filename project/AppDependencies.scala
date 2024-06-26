import sbt.*

private object AppDependencies {

  import play.sbt.PlayImport.*

  val compile: Seq[ModuleID] = Seq(
    ehcache,
    // claims to be a frontend even though it's definitely a backend so it has access to `sso.encryption.key`,
    // and because it appears to need to set cookies
    "uk.gov.hmrc" %% "bootstrap-frontend-play-30" % "8.5.0"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"   % "8.5.0"    % Test,
    "org.mockito"            %% "mockito-scala-scalatest"  % "1.17.31"  % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test
}
