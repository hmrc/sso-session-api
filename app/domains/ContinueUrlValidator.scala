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

package domains

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{OnlyRelative, RedirectUrl, SafeRedirectUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl._

import scala.concurrent.Future

@Singleton
class ContinueUrlValidator @Inject() (whiteListService: WhiteListService) {

  def getRelativeOrAbsoluteWhiteListed(continueUrl: RedirectUrl)(implicit hc: HeaderCarrier): Future[Option[SafeRedirectUrl]] = {
    continueUrl.getEither(OnlyRelative) match {
      case Left(_) =>
        whiteListService.getWhitelistedAbsoluteUrl(continueUrl)
      case Right(safe) =>
        Future.successful(Some(safe))
    }
  }
}
