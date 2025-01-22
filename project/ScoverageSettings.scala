import sbt.Keys.parallelExecution
import sbt.{Def, *}
import scoverage.ScoverageKeys

object ScoverageSettings {
  def apply(): Seq[Def.Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;.*(config|views.*);.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimumStmtTotal := 60,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    Test / parallelExecution := false,
    ScoverageKeys.coverageExcludedFiles := Seq(
      "<empty>",
      "Reverse.*",
      ".*models.*",
      ".*repositories.*",
      ".*BuildInfo.*",
      ".*javascript.*",
      ".*Routes.*",
      ".*GuiceInjector",
      ".*Test.*"
    ).mkString(";")
  )
}
