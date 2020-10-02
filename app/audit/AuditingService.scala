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

package audit

import javax.inject.Inject
import play.api.mvc.Request
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

class AuditingService @Inject() (
    auditConnector: AuditConnector
)(implicit ec: ExecutionContext) extends FrontendHeaderCarrierProvider {

  def sendTokenCreatedEvent(continueUrl: String)(implicit request: Request[_]): Future[Unit] = {
    sendEvent(
      "api-sso-token-created",
      "api-sso-token-created",
      Map(
        "continueUrl" -> continueUrl,
        "bearer-token" -> hc.authorization.map(_.value).getOrElse("-"),
        "session-id" -> hc.sessionId.map(_.value).getOrElse("-")
      )
    )
  }

  private def sendEvent(
      auditType:       String,
      transactionName: String,
      details:         Map[String, String]
  )(implicit request: Request[_]): Future[Unit] = {
    val event = DataEvent(
      "sso-session-api",
      auditType,
      detail = details,
      tags   = hc.toAuditTags(transactionName, request.path)
    )

    auditConnector.sendEvent(event).map(_ => ())
  }

}
