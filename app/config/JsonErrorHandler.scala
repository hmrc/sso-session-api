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

package config

import javax.inject.Inject
import play.api.Logging
import play.api.http.HttpErrorHandler
import play.api.http.Status.*
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Results.*
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.auth.core.AuthorisationException
import uk.gov.hmrc.http.*
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.HttpAuditEvent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

class JsonErrorHandler @Inject() (
  auditConnector: AuditConnector,
  httpAuditEvent: HttpAuditEvent
)(implicit ec: ExecutionContext)
    extends HttpErrorHandler
    with FrontendHeaderCarrierProvider
    with Logging {

  import httpAuditEvent.dataEvent

  override def onClientError(request: RequestHeader, statusCode: Int, message: String): Future[Result] =
    Future.successful {
      implicit val headerCarrier: HeaderCarrier = hc(request)
      statusCode match {
        case NOT_FOUND =>
          auditConnector.sendEvent(
            dataEvent(
              eventType       = "ResourceNotFound",
              transactionName = "Resource Endpoint Not Found",
              request         = request,
              detail          = Map.empty
            )
          )
          NotFound(Json.toJson(ErrorResponse(NOT_FOUND, "URI not found", requested = Some(request.path), xStatusCode = None)))
        case BAD_REQUEST =>
          auditConnector.sendEvent(
            dataEvent(
              eventType       = "ServerValidationError",
              transactionName = "Request bad format exception",
              request         = request,
              detail          = Map.empty
            )
          )
          BadRequest(Json.toJson(ErrorResponse(BAD_REQUEST, "bad request", None, None)))
        case _ =>
          auditConnector.sendEvent(
            dataEvent(
              eventType       = "ClientError",
              transactionName = s"A client error occurred, status: $statusCode",
              request         = request,
              detail          = Map.empty
            )
          )
          Status(statusCode)(Json.toJson(ErrorResponse(statusCode, message, None, None)))
      }
    }

  override def onServerError(request: RequestHeader, ex: Throwable): Future[Result] = {
    implicit val headerCarrier: HeaderCarrier = hc(request)

    val message = s"! Internal server error, for (${request.method}) [${request.uri}] -> "
    val eventType = ex match {
      case _: NotFoundException      => "ResourceNotFound"
      case _: AuthorisationException => "ClientError"
      case _: JsValidationException  => "ServerValidationError"
      case _                         => "ServerInternalError"
    }

    val errorResponse = ex match {
      case e: AuthorisationException =>
        logger.error(message, e)
        ErrorResponse(401, e.getMessage, None, None)
      case e: HttpException =>
        logger.error(e.getMessage, e)
        ErrorResponse(e.responseCode, e.getMessage, None, None)
      case e: UpstreamErrorResponse =>
        logger.error(e.getMessage, e)
        ErrorResponse(e.reportAs, e.getMessage, None, None)
      case e: Throwable =>
        logger.error(message, e)
        ErrorResponse(INTERNAL_SERVER_ERROR, e.getMessage, None, None)
    }

    auditConnector.sendEvent(
      dataEvent(
        eventType       = eventType,
        transactionName = "Unexpected error",
        request         = request,
        detail          = Map("transactionFailureReason" -> ex.getMessage)
      )
    )
    Future.successful(new Status(errorResponse.statusCode)(Json.toJson(errorResponse)))
  }

  case class ErrorResponse(
    statusCode: Int,
    message: String,
    xStatusCode: Option[String],
    requested: Option[String]
  )

  object ErrorResponse {
    implicit val format: OFormat[ErrorResponse] = Json.format[ErrorResponse]
  }
}
