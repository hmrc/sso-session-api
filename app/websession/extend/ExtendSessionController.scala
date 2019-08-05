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

package websession.extend

import org.apache.commons.codec.binary.Base64
import org.joda.time.{DateTime, DateTimeZone, Duration}
import play.api.mvc.{Action, AnyContent, Session}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.frontend.auth._
import uk.gov.hmrc.play.frontend.controller.{ActionWithMdc, FrontendController, UnauthorisedAction}
import uk.gov.hmrc.time.DateTimeUtils

class ExtendSessionController extends FrontendController {
  val base64TransparentImage = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAABBJREFUeNpi+P//PwNAgAEACPwC/tuiTRYAAAAASUVORK5CYII="
  val image = Base64.decodeBase64(base64TransparentImage.getBytes)
  val imageResponse =
    Ok(image)
      .withHeaders(
        "Pragma-directive" -> "no-cache",
        "Cache-directive" -> "no-cache",
        "Cache-control" -> "no-cache",
        "Pragma" -> "no-cache",
        "Expires" -> "0"
      )
      .as("image/png")

  def extend(guid: String) = WithSessionTimeoutUpdatedForValidSession {
    UnauthorisedAction { implicit request => imageResponse }
  }
}

object ExtendSessionController extends ExtendSessionController

class WithSessionTimeoutUpdatedForValidSession(val now: () => DateTime) extends SessionTimeout with SessionTimestamp {
  def apply(action: Action[AnyContent]): Action[AnyContent] = ActionWithMdc.async { implicit request =>
    if (request.session.isEmpty || userNeedsNewSession(request.session, now))
      action(request)
    else
      addTimestamp(request, action(request))
  }
}

object WithSessionTimeoutUpdatedForValidSession extends WithSessionTimeoutUpdatedForValidSession(() => DateTimeUtils.now)

trait SessionTimestamp {
  val defaultTimeoutSeconds = 900

  final def userNeedsNewSession(session: Session, now: () => DateTime) = extractTimestamp(session) match {
    case None     => false
    case Some(ts) => hasExpired(now)(ts)
  }

  private def extractTimestamp(session: Session) =
    try {
      session.get(SessionKeys.lastRequestTimestamp) map (t => new DateTime(t.toLong, DateTimeZone.UTC))
    } catch {
      case e: NumberFormatException => None
    }

  private def hasExpired(now: () => DateTime)(timestamp: DateTime) =
    now() isAfter (timestamp plus Duration.standardSeconds(defaultTimeoutSeconds))
}
