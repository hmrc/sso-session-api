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

package service

import connectors.SsoDomainsConnector
import models.{DomainsResponse, PermittedDomains}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{lenient, verify, verifyNoInteractions, verifyNoMoreInteractions, when}
import play.api.cache.AsyncCacheApi
import services.CachedDomainsService
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.{Duration, SECONDS}
import scala.concurrent.{ExecutionContext, Future}

class CachedDomainsServiceSpec extends UnitSpec {

  trait Setup {
    val mockSsoDomainsConnector: SsoDomainsConnector = mock[SsoDomainsConnector]
    val mockCache: AsyncCacheApi = mock[AsyncCacheApi]

    val domainsFromRealSSO: DomainsResponse = DomainsResponse(PermittedDomains(Set.empty, Set.empty), 123)
    val ssoResponseWithSuggestedTTL: (DomainsResponse, Some[String]) = (domainsFromRealSSO, Some("max-age=123"))
    val ssoResponseWithNoTTL: (DomainsResponse, None.type) = (domainsFromRealSSO, None)
    val cachedDomains: DomainsResponse = DomainsResponse(PermittedDomains(Set.empty, Set.empty), 0)

    val cachedDomainsService = new CachedDomainsService(mockSsoDomainsConnector, mockCache)(ExecutionContext.global)
  }

  "white list service" should {

    "delegate to the real connector when the cache misses" in new Setup {
      when(mockCache.get[DomainsResponse]("sso/domains")).thenReturn(Future.successful(None))
      lenient().when(mockCache.set(eqTo("sso/domains"), any, any)).thenReturn(Future.successful(Done))
      when(mockSsoDomainsConnector.getDomains()(any)).thenReturn(Future.successful(domainsFromRealSSO))

      await(cachedDomainsService.getDomains()(HeaderCarrier())) shouldBe Some(domainsFromRealSSO)
    }

    "return the cached value if the cache is populated" in new Setup {
      when(mockCache.get[DomainsResponse]("sso/domains")).thenReturn(Future.successful(Some(cachedDomains)))
      lenient().when(mockCache.set(eqTo("sso/domains"), any, any)).thenReturn(Future.successful(Done))

      await(cachedDomainsService.getDomains()(HeaderCarrier())) shouldBe Some(cachedDomains)
      verifyNoInteractions(mockSsoDomainsConnector)
    }

    "populate the cache when a call to the real connector succeeds" in new Setup {
      when(mockCache.get[DomainsResponse]("sso/domains")).thenReturn(Future.successful(None))
      when(mockSsoDomainsConnector.getDomains()(any)).thenReturn(Future.successful(domainsFromRealSSO))

      await(cachedDomainsService.getDomains()(HeaderCarrier()))

      verify(mockCache).set("sso/domains", domainsFromRealSSO, Duration(123, SECONDS))
    }

    "The cache ttl should be suggested by the sso connector" in new Setup {
      when(mockCache.get[DomainsResponse]("sso/domains")).thenReturn(Future.successful(None))
      when(mockSsoDomainsConnector.getDomains()(any)).thenReturn(Future.successful(domainsFromRealSSO))

      await(cachedDomainsService.getDomains()(HeaderCarrier()))

      verify(mockCache).set("sso/domains", domainsFromRealSSO, Duration(123, SECONDS))
    }

    "do not cache failed calls the end point as we do not want temporary call failures to become long lasting failures" in new Setup {
      when(mockCache.get[DomainsResponse]("sso/domains")).thenReturn(Future.successful(None))
      when(mockSsoDomainsConnector.getDomains()(any)).thenReturn(Future.failed(new Exception("")))

      await(cachedDomainsService.getDomains()(HeaderCarrier()))

      verify(mockCache).get[DomainsResponse]("sso/domains")
      verifyNoMoreInteractions(mockCache)
    }

  }

}
