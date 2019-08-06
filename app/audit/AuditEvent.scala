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

package audit

import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.AuditExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent

object AuditEvent {

  val auditSource = "sso-session-api"

  private def getTags(hc: HeaderCarrier, path: String, transactionName: String): Map[String, String] = Map(
    "clientIP" -> hc.trueClientIp.getOrElse("-"),
    "clientPort" -> hc.trueClientPort.getOrElse("-")) ++
    hc.headers
    .filterNot(_._1 == HeaderNames.token)
    .filterNot(_._1 == HeaderNames.trueClientIp)
    .filterNot(_._1 == HeaderNames.trueClientPort)
    .toMap ++
    hc.toAuditTags(transactionName, path)

  def create(transactionName: String, continueUrl: Option[String] = None)(implicit hc: HeaderCarrier, request: Request[_]) = {
    DataEvent(
      auditType   = transactionName,
      tags        = getTags(hc, request.path, transactionName) ++ Map("type" -> "Audit"),
      detail      =
        hc.toAuditDetails()
          ++ Map(
            "continueUrl" -> continueUrl.fold("-"){ url => url },
            "bearer-token" -> hc.authorization.fold("-"){ _.value },
            "session-id" -> hc.sessionId.fold("-"){ _.value },
            "user-id" -> hc.userId.fold("-"){ _.value }
          ),
      auditSource = auditSource)
  }

}
