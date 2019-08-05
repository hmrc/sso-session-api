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

package websession.redeem

import java.net.URL

import javax.inject.{Inject, Singleton}
import audit.AuditEvent
import auth.FrontendAuthConnector
import connectors.SsoConnector
import play.api.Logger
import play.api.libs.json.{JsPath, Reads}
import uk.gov.hmrc.config.{SsoCrypto, SsoFrontendAuditConnector}
import uk.gov.hmrc.crypto.Crypted
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, NotFoundException, SessionKeys}
import uk.gov.hmrc.play.frontend.auth.{AuthContext, AuthenticationProviderIds}
import uk.gov.hmrc.play.frontend.controller.{ActionWithMdc, FrontendController}
import uk.gov.hmrc.time.DateTimeUtils
import websession.ApiToken

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class RedeemTokenController @Inject() (val ssoConnector:              SsoConnector,
                                       val ssoCrypto:                 SsoCrypto,
                                       val ssoFrontendAuditConnector: SsoFrontendAuditConnector,
                                       val auth:                      FrontendAuthConnector) extends FrontendController {

  def redeem(token: String) = ActionWithMdc.async { implicit request =>
    val encryptedToken = Crypted.fromBase64(token)
    Try {
      new URL(ssoCrypto.decrypt(encryptedToken).value)
    } match {
      case Success(tokenUrl) =>
        (for {
          apiToken <- ssoConnector.getTokenDetails(tokenUrl)
          _ = ssoFrontendAuditConnector.sendEvent(AuditEvent.create("api-sso-token-redeemed", Some(apiToken.continueUrl)))
          authProvider <- getAuthProvider(apiToken)
        } yield SeeOther(apiToken.continueUrl)
          .withSession(
            SessionKeys.sessionId -> apiToken.sessionId,
            SessionKeys.userId -> apiToken.userId,
            SessionKeys.authToken -> apiToken.bearerToken,
            SessionKeys.token -> "dummy",
            SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.getMillis.toString,
            SessionKeys.authProvider -> authProvider
          )).recoverWith {
          case e: NotFoundException =>
            ssoFrontendAuditConnector.sendEvent(AuditEvent.create("api-sso-token-hack-attempt"))
            Future.successful(NotFound)
        }
      case Failure(e) =>
        ssoFrontendAuditConnector.sendEvent(AuditEvent.create("api-sso-token-hack-attempt"))
        Future.successful(NotFound)
    }
  }

  private def getAuthProvider(apiToken: ApiToken)(implicit hc: HeaderCarrier): Future[String] = {
      implicit def authProviderReads: Reads[String] = (JsPath \ "authProviderType").read[String](Reads.StringReads)

    auth.getAuthority(apiToken.userId) flatMap {
      case Some(authority) =>
        auth.getUserDetails[String](AuthContext(authority)) map {
          case "Verify"            => AuthenticationProviderIds.VerifyProviderId
          case "GovernmentGateway" => AuthenticationProviderIds.GovernmentGatewayId
          case other               => Logger.warn(s"""[GG-3277] unrecognised authProviderType $other - defaulting to "api""""); "api"
        }
      case None =>
        Logger.warn(s"""[GG-3277] no auth record for api token $apiToken""")
        Future.successful("api")
    }
  }

}
