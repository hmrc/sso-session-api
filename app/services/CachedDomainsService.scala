/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.SsoDomainsConnector
import javax.inject.{Inject, Singleton}
import models.DomainsResponse
import play.api.Logging
import play.api.cache.AsyncCacheApi
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CachedDomainsService @Inject() (
    ssoDomainsConnector: SsoDomainsConnector,
    cache:               AsyncCacheApi
)(implicit ec: ExecutionContext) extends Logging {

  private val CacheKey = "sso/domains"

  def getDomains()(implicit hc: HeaderCarrier): Future[Option[DomainsResponse]] = {
    cache.get[DomainsResponse](CacheKey).flatMap {
      case Some(cachedDomainsResponse) =>
        Future.successful(Some(cachedDomainsResponse))
      case None =>
        ssoDomainsConnector.getDomains.map { domainsResponse =>
          cache.set(CacheKey, domainsResponse, Duration(domainsResponse.maxAge, SECONDS))
          Some(domainsResponse)
        }.recover {
          case _: Exception =>
            logger.warn("List of valid domains is unavailable (the domains service may be down). Defaulting to not valid.")
            None
        }
    }
  }
}
