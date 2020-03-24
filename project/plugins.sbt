resolvers += Resolver.url("HMRC Sbt Plugin Releases", url("https://dl.bintray.com/hmrc/sbt-plugin-releases"))(Resolver.ivyStylePatterns)
resolvers += "HMRC Releases" at "https://dl.bintray.com/hmrc/releases"
resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.4")

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.24")

addSbtPlugin("uk.gov.hmrc" % "sbt-settings" % "3.11.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-distributables" % "1.6.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-service-manager" % "0.5.0")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

addSbtPlugin("org.wartremover" % "sbt-wartremover" % "2.3.1")

addSbtPlugin("uk.gov.hmrc" % "sbt-auto-build" % "1.16.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-git-versioning" % "1.20.0")

addSbtPlugin("uk.gov.hmrc" % "sbt-artifactory" % "0.21.0")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")