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

import javax.inject.{Inject, Singleton}

import connectors.SsoDomainsConnector
import play.api.Logger
import play.api.cache.CacheApi
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}

@Singleton
class CachedDomainsService @Inject() (ssoDomainsConnector: SsoDomainsConnector, cache: CacheApi) {

  private val CacheKey = "sso/domains"

  def getDomains()(implicit hc: HeaderCarrier): Future[Option[DomainsResponse]] = {
    cache.get[DomainsResponse](CacheKey).map { cachedDomainsResponse =>
      Future.successful(Some(cachedDomainsResponse))
    }.getOrElse {
      ssoDomainsConnector.getDomains.map { domainsResponse =>
        cache.set(CacheKey, domainsResponse, Duration(domainsResponse.maxAge, SECONDS))
        Some(domainsResponse)
      }.recover{
        case e: Exception =>
          Logger.warn("List of valid domains is unavailable (the domains service may be down). Defaulting to not valid.")
          None
      }
    }
  }

}
