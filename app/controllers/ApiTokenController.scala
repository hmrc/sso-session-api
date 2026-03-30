/*
 * Copyright 2024 HM Revenue & Customs
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

import audit.AuditingService
import config.*
import connectors.SsoConnector
import models.ApiToken
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.request.{Cell, RequestAttrKey}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, RequestHeader, Result}
import services.{ContinueUrlValidator, PermittedContinueUrl}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.http.{BadRequestException, HeaderNames, SessionKeys, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.net.URL
import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class ApiTokenController @Inject() (
  ssoConnector: SsoConnector,
  authConnector: AuthConnector,
  auditingService: AuditingService,
  frontendAppConfig: AppConfig,
  val continueUrlValidator: ContinueUrlValidator,
  controllerComponents: MessagesControllerComponents
)(implicit val ec: ExecutionContext)
    extends FrontendController(controllerComponents)
    with PermittedContinueUrl
    with Logging {

  // [GG-9130] Determine if there is an existing sessionId that must be used, and if not, generate one.
  private def withExistingOrNewSessionId(f: RequestHeader => Future[Result])(implicit request: RequestHeader): Future[Result] = {
    authConnector
      .authorise(EmptyPredicate, Retrievals.mdtpInformation)(HeaderCarrierConverter.fromRequest(request), implicitly[ExecutionContext])
      .transform {
        // [GG-9130] If sessionId exists in auth against the BT use this, otherwise use the provided header version from the caller (Pega et al.). If this is not set, generate one.
        case Success(Some(mdtpInformation)) => Success(mdtpInformation.sessionId)
        case otherResult =>
          if (otherResult == Success(None))
            logger.warn(s"[GG-9130] SSO token request - auth record found but no sessionId, requestId : ${hc.requestId}")
          val sessionIdFromRequestHeader: Option[String] = hc.sessionId.map(_.value)
          Success(sessionIdFromRequestHeader.getOrElse {
            val generatedSessionId = s"session-${UUID.randomUUID().toString}"
            logger.warn(
              s"[GG-9130] SSO token request - no sessionId found in auth or request, generating sessionId : $generatedSessionId, requestId : ${hc.requestId}"
            )
            generatedSessionId
          })
      }
      .flatMap { modifiedSessionId =>
        val session = {
          val sessionWithAuthToken = request.headers.get(HeaderNames.authorisation) match {
            case Some(authToken) => request.session + (SessionKeys.authToken -> authToken)
            case None            => request.session
          }
          sessionWithAuthToken + (SessionKeys.sessionId -> modifiedSessionId)
        }
        implicit val modifiedRequest: RequestHeader = {
          val headers = request.headers.replace(HeaderNames.xSessionId -> modifiedSessionId)
          request
            .withHeaders(headers)
            .addAttr(RequestAttrKey.Session, Cell(session))
        }

        f(modifiedRequest).map(_.withSession(session))
      }
  }

  def create(continueUrl: RedirectUrl): Action[AnyContent] = Action.async { implicit request =>
    withExistingOrNewSessionId { implicit modifiedRequest =>
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
      }
    }.recover {
      case UpstreamErrorResponse(_, UNAUTHORIZED, _, headers) => Unauthorized.withHeaders(unGroup(headers.toSeq)*)
      case UpstreamErrorResponse(_, FORBIDDEN, _, headers)    => Forbidden.withHeaders(unGroup(headers.toSeq)*)
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
