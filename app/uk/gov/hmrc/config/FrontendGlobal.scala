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

import com.typesafe.config.Config
import javax.inject.{Inject, Singleton}
import net.ceedubs.ficus.Ficus._
import play.api._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.gg.config.GenericAppConfig
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHook
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters.{FrontendAuditFilter, FrontendLoggingFilter, MicroserviceFilterSupport, SessionTimeoutFilter}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

object FrontendGlobal extends DefaultFrontendGlobal with I18nSupport with RunMode with GenericAppConfig {

  override lazy val auditConnector: AuditConnector = SsoFrontendAuditConnector
  override lazy val loggingFilter: FrontendLoggingFilter = LoggingFilter
  override lazy val frontendAuditFilter: FrontendAuditFilter = AuditFilter

  lazy val messagesApi = Play.current.injector.instanceOf[MessagesApi]

  override def onStart(app: Application) {
    super.onStart(app)
    val applicationCrypto = new ApplicationCrypto(runModeConfiguration.underlying)
    applicationCrypto.verifyConfiguration()
  }

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
    uk.gov.hmrc.views.html.error_template(pageTitle, heading, message, FrontendAppConfig.analyticsHost,
                                          Some(FrontendAppConfig.analyticsToken),
                                          FrontendAppConfig.reportAProblemPartialUrl, FrontendAppConfig.reportAProblemNonJSUrl)

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"$env.microservice.metrics")

  override def sessionTimeoutFilter: SessionTimeoutFilter = {
    val timeoutDuration = FrontendAppConfig.timeoutDuration
    val wipeIdleSession = FrontendAppConfig.wipeIdleSession
    val additionalSessionKeysToKeep = FrontendAppConfig.additionalSessionKeysToKeep
    val whitelistedPaths = FrontendAppConfig.whitelistedPaths

    new SessionTimeoutFilterWithWhitelist(
      timeoutDuration             = timeoutDuration,
      additionalSessionKeysToKeep = additionalSessionKeysToKeep,
      onlyWipeAuthToken           = !wipeIdleSession,
      whitelistedPaths            = whitelistedPaths
    )
  }
}

object ControllerConfiguration extends ControllerConfig {
  val controllerConfigs: Config = FrontendGlobal.runModeConfiguration.underlying.as[Config]("controllers")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object SsoFrontendAuditConnector extends SsoFrontendAuditConnector

class SsoFrontendAuditConnector extends AuditConnector with AppName with RunMode with GenericAppConfig {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}

class WSHttp extends WSGet with WSPut with WSPost with WSDelete with HttpGet with HttpPut with HttpPost with HttpDelete with AppName with RunMode with GenericAppConfig {
  override val hooks: Seq[HttpHook] = NoneRequired

  override protected def configuration: Option[Config] = Option(FrontendGlobal.runModeConfiguration.underlying)
}

object WSHttp extends WSHttp

object AuditFilter extends FrontendAuditFilter with MicroserviceFilterSupport with RunMode with AppName with GenericAppConfig {
  override val maskedFormFields: Seq[String] = Seq("password")
  override val applicationPort: Option[Int] = None
  override val auditConnector: AuditConnector = SsoFrontendAuditConnector
  override def controllerNeedsAuditing(controllerName: String): Boolean = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

@Singleton
class SsoCrypto @Inject() (appConfig: FrontendAppConfig) extends Decrypter with Encrypter {
  val crypto = new CryptoGCMWithKeysFromConfig(baseConfigKey = "sso.encryption", FrontendGlobal.runModeConfiguration.underlying)
  override def decrypt(reversiblyEncrypted: Crypted): PlainText = crypto.decrypt(reversiblyEncrypted)
  override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = crypto.decryptAsBytes(reversiblyEncrypted)
  override def encrypt(plain: PlainContent): Crypted = crypto.encrypt(plain)
}
