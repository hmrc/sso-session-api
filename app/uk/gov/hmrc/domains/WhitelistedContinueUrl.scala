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

package uk.gov.hmrc.domains

import play.api.mvc.Result
import play.api.mvc.Results.BadRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait WhitelistedContinueUrl {

  def continueUrlValidator: ContinueUrlValidator

  def withWhitelistedContinueUrl(continue: ContinueUrl*)(body: => Future[Result])(implicit hc: HeaderCarrier) = {
    Future.sequence(continue.map { value =>
      continueUrlValidator.isRelativeOrAbsoluteWhiteListed(value)
    }).flatMap { boolList =>
      if (boolList.forall(identity)) body
      else Future.successful(BadRequest("Invalid Continue URL"))
    }
  }

}
