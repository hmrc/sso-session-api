/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import java.net.URL
import java.util.UUID
import audit.AuditingService
import config._
import connectors.SsoConnector

import javax.inject.{Inject, Singleton}
import models.ApiToken
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{ContinueUrlValidator, PermittedContinueUrl}
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, SessionId, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

@Singleton
class ApiTokenController @Inject() (
  ssoConnector:             SsoConnector,
  auditingService:          AuditingService,
  frontendAppConfig:        AppConfig,
  val continueUrlValidator: ContinueUrlValidator,
  controllerComponents:     MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(controllerComponents)
    with PermittedContinueUrl {

  def create(continueUrl: RedirectUrl): Action[AnyContent] = Action.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequest(request)
      .copy(sessionId = Some(SessionId(s"session-${UUID.randomUUID().toString}")))

    withPermittedContinueUrl(continueUrl) { permittedUrl =>
      val maybeApiToken = for {
        bearer    <- hc.authorization
        sessionId <- hc.sessionId
      } yield ApiToken(bearer.value, sessionId.value, permittedUrl.url, Some("deprecated"))

      maybeApiToken.fold {
        throw new BadRequestException("No Authorisation header in the request")
      } { apiToken =>
        for {
          tokenUrl <- ssoConnector.createToken(apiToken)
          _ = auditingService.sendTokenCreatedEvent(permittedUrl.url)
        } yield Ok(
          Json.obj(
            "_links" -> Json.obj(
              "session" -> redeemUrl(tokenUrl)
            )
          )
        )
      }
    }.recover {
      case UpstreamErrorResponse(_, UNAUTHORIZED, _, headers) => Unauthorized.withHeaders(unGroup(headers.toSeq): _*)
      case UpstreamErrorResponse(_, FORBIDDEN, _, headers)    => Forbidden.withHeaders(unGroup(headers.toSeq): _*)
      case UpstreamErrorResponse(message, BAD_REQUEST, _, _)  => BadRequest(message)
    }
  }

  private def redeemUrl(tokenUrl: URL) = {
    val encrypted = new String(frontendAppConfig.encrypt(PlainText(tokenUrl.toString)).toBase64)
    s"${frontendAppConfig.ssoFeHost}/sso/session?token=$encrypted"
  }

  private def unGroup[T, U](in: Seq[(T, Seq[U])]): Seq[(T, U)] = for {
    (key, values) <- in
    value         <- values
  } yield key -> value

}
