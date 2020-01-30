/*
 * Copyright 2020 HM Revenue & Customs
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

package websession.create

import java.net.URL
import java.util.UUID

import javax.inject.{Inject, Singleton}
import audit.AuditEvent
import auth.FrontendAuthConnector
import connectors.SsoConnector
import play.api.libs.json.Json
import play.api.libs.json.Json.JsValueWrapper
import play.api.mvc.{Action, AnyContent, Result}
import config._
import uk.gov.hmrc.crypto.PlainText
import domains.{ContinueUrlValidator, WhitelistedContinueUrl}
import uk.gov.hmrc.http.logging.SessionId
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, Upstream4xxResponse}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.bootstrap.controller.FrontendController
import websession.ApiToken

import scala.concurrent.Future

@Singleton
class ApiTokenController @Inject() (val ssoConnector:         SsoConnector,
                                    val authConnector:        FrontendAuthConnector,
                                    val auditConnector:       AuditConnector,
                                    val frontendAppConfig:    FrontendAppConfig,
                                    val continueUrlValidator: ContinueUrlValidator
) extends FrontendController with WhitelistedContinueUrl {

  def create(continueUrl: ContinueUrl): Action[AnyContent] = Action.async { implicit request =>

      def redeemUrl(tokenUrl: URL): JsValueWrapper = {
        val encrypted = new String(frontendAppConfig.encrypt(PlainText(tokenUrl.toString)).toBase64)
        s"${frontendAppConfig.ssoFeHost}/sso/session?token=$encrypted"
      }

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, None)
      .copy(sessionId = Some(SessionId(s"session-${UUID.randomUUID().toString}")))

    authConnector.getAuthUri().flatMap {
      case Some(authResponse) => withWhitelistedContinueUrl(continueUrl) {
        val maybeApiToken = for {
          bearer <- hc.authorization
          sessionId <- hc.sessionId
        } yield ApiToken(bearer.value, sessionId.value, continueUrl.url, authResponse.uri)

        maybeApiToken.fold {
          throw new BadRequestException("No Authorisation header in the request")

        } { apiToken =>
          for {
            tokenUrl <- ssoConnector.createToken(apiToken)
            _ = auditConnector.sendEvent(AuditEvent.create("api-sso-token-created", Some(continueUrl.url)))
          } yield Ok(
            Json.obj(
              "_links" -> Json.obj(
                "session" -> redeemUrl(tokenUrl)
              )
            ))
        }
      }
      case _ => Future.successful(Unauthorized)
    }.recover {
      case Upstream4xxResponse(_, UNAUTHORIZED, _, headers) => Unauthorized.withHeaders(unGroup(headers.toSeq): _*)
      case Upstream4xxResponse(_, FORBIDDEN, _, headers)    => Forbidden.withHeaders(unGroup(headers.toSeq): _*)
      case e: BadRequestException                           => BadRequest(e.message)
    }
  }

  private def unGroup[T, U](in: Seq[(T, Seq[U])]): Seq[(T, U)] = for {
    (key, values) <- in
    value <- values
  } yield key -> value

}
