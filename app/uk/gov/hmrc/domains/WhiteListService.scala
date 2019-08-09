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

import java.net.URL
import javax.inject.{Inject, Singleton}

import connectors.SsoConnector
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.Try

@Singleton
class WhiteListService @Inject() (cachedDomainService: CachedDomainsService, ssoConnector: SsoConnector) {

  def hasExternalDomain(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = isAbsoluteUrlWhiteListed(continueUrl, _.externalDomains)

  def hasInternalDomain(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = isAbsoluteUrlWhiteListed(continueUrl, _.internalDomains)

  def isAbsoluteUrlWhiteListed(continueUrl: ContinueUrl, f: DomainsResponse => Set[String] = _.allDomains)(implicit hc: HeaderCarrier): Future[Boolean] = {

      def isValidURL(url: String) = Try(new URL(url)).isSuccess

    if (continueUrl.isAbsoluteUrl && isValidURL(continueUrl.url)) {
      cachedDomainService.getDomains.map { validDomainsOption =>
        validDomainsOption.map { validDomains =>
          val host = new URL(continueUrl.url).getHost
          f(validDomains).contains(host)
        }.getOrElse {
          Logger.warn("List of valid domains is unavailable (the domains service may be down). Defaulting to not valid.")
          false
        }
      }

    } else {
      Future.successful(false)
    }
  }

}
