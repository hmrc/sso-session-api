import sbt.Compile
import sbt.Keys.{credentials, packageDoc, packageSrc, publishArtifact}

object Repositories {

  import uk.gov.hmrc._
  import PublishingSettings._

  lazy val playPublishingSettings = Seq(
    credentials += SbtCredentials,
    publishArtifact in(Compile, packageDoc) := false,
    publishArtifact in(Compile, packageSrc) := false
  ) ++
    publishAllArtefacts
}
