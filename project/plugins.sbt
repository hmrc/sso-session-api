resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.playframework"    % "sbt-plugin"              % "3.0.5")

addSbtPlugin("uk.gov.hmrc"          % "sbt-distributables"      % "2.5.0")

addSbtPlugin("org.scoverage"        % "sbt-scoverage"           % "2.2.0")

addSbtPlugin("org.scalameta"        % "sbt-scalafmt"            % "2.5.2")

addSbtPlugin("uk.gov.hmrc"          % "sbt-auto-build"          % "3.22.0")
