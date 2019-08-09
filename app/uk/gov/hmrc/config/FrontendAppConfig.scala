/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.config

import org.joda.time.Duration
import play.api.Configuration
import play.api.Mode.Mode
import play.api.Play.{configuration, current}
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}

object FrontendAppConfig extends FrontendAppConfig

class FrontendAppConfig extends ServicesConfig with RunMode {

  private val contactHost = configuration.getString("contact-frontend.host").getOrElse("")
  private val contactFormServiceIdentifier = "sso-session-api"

  val analyticsHost = configuration.getString(s"$env.google-analytics.host").getOrElse("service.gov.uk")

  lazy val appName = loadConfig("appName")
  lazy val appUrl = loadConfig("appUrl")
  lazy val serviceLocatorUrl = baseUrl("service-locator")

  def isRegistrationEnabled = getConfBool("service-locator.enabled", true)

  lazy val analyticsToken = loadConfig("google-analytics.token")
  lazy val wipeIdleSession = configuration.getBoolean("session.wipeIdleSession").getOrElse(true)
  lazy val additionalSessionKeysToKeep = configuration.getStringSeq("session.additionalSessionKeysToKeep").getOrElse(Seq.empty).toSet
  lazy val whitelistedPaths = configuration.getStringSeq("session.whitelisted.paths").getOrElse(Seq.empty).toSet

  lazy val timeoutDuration = configuration.getLong("session.timeoutSeconds")
    .map(Duration.standardSeconds)
    .getOrElse(Duration.standardMinutes(15))

  lazy val reportAProblemPartialUrl = s"$contactHost/contact/problem_reports_ajax?service=$contactFormServiceIdentifier"
  lazy val reportAProblemNonJSUrl = s"$contactHost/contact/problem_reports_nonjs?service=$contactFormServiceIdentifier"

  lazy val ssoFeHost = configuration.getString(s"$env.sso-fe.host").getOrElse("")

  private def loadConfig(key: String): String = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  override protected def mode: Mode = FrontendGlobal.mode

  override protected def runModeConfiguration: Configuration = FrontendGlobal.runModeConfiguration
}

