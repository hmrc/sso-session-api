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

package services

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, RedirectUrl, SafeRedirectUrl}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AllowlistService @Inject() (cachedDomainService: CachedDomainsService)(implicit ec: ExecutionContext) extends Logging {

  def getPermittedAbsoluteUrl(continueUrl: RedirectUrl)(implicit hc: HeaderCarrier): Future[Option[SafeRedirectUrl]] = {
    cachedDomainService.getDomains().map {
      case Some(validDomains) =>
        continueUrl.getEither(AbsoluteWithHostnameFromAllowlist(validDomains.allDomains)).toOption

      case None =>
        logger.warn("List of valid domains is unavailable (the domains service may be down). Defaulting to not valid.")
        None
    }
  }
}
