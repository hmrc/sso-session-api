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

package service

import connectors.SsoDomainsConnector
import domains.{CachedDomainsService, DomainsResponse, WhiteListedDomains}
import org.scalatest.concurrent.ScalaFutures
import play.api.cache.CacheApi
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}

class CachedDomainsServiceSpec extends UnitSpec with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val ssoDomainsConnectorMock = mock[SsoDomainsConnector]
    val cacheMock = mock[CacheApi]

    val domainsFromRealSSO = mock[DomainsResponse]
    val ssoResponseWithSuggestedTTL = (domainsFromRealSSO, Some("max-age=123"))
    val ssoResponseWithNoTTL = (domainsFromRealSSO, None)
    val cachedDomains = DomainsResponse(WhiteListedDomains(Set.empty, Set.empty), 0)

    val cachedDomainsService = new CachedDomainsService(ssoDomainsConnectorMock, cacheMock)
  }

  "white list service" should {

    "delegate to the real connector which the cache misses" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(None)
      when(cacheMock.set(eqTo("sso/domains"), any, any)).isLenient()
      when(ssoDomainsConnectorMock.getDomains()(any[HeaderCarrier])).thenReturn(Future.successful(domainsFromRealSSO))
      when(domainsFromRealSSO.maxAge).thenReturn(123)
      cachedDomainsService.getDomains().futureValue shouldBe Some(domainsFromRealSSO)
    }

    "return the cached value if the cache is populated" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(Some(cachedDomains))
      when(cacheMock.set(eqTo("sso/domains"), any, any)).isLenient()
      cachedDomainsService.getDomains().futureValue shouldBe Some(cachedDomains)
      verifyZeroInteractions(ssoDomainsConnectorMock)
    }

    "populate the cache when a call to the real connector succeeds" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(None)
      when(ssoDomainsConnectorMock.getDomains()(any[HeaderCarrier])).thenReturn(Future.successful(domainsFromRealSSO))
      when(domainsFromRealSSO.maxAge).thenReturn(123)
      cachedDomainsService.getDomains().futureValue

      verify(cacheMock).set("sso/domains", domainsFromRealSSO, Duration(123, SECONDS))
    }

    "The cache ttl should be suggested by the sso connector" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(None)
      when(ssoDomainsConnectorMock.getDomains()(any[HeaderCarrier])).thenReturn(Future.successful(domainsFromRealSSO))
      when(domainsFromRealSSO.maxAge).thenReturn(123)
      cachedDomainsService.getDomains().futureValue

      verify(cacheMock).set("sso/domains", domainsFromRealSSO, Duration(123, SECONDS))
    }

    "use a sensible default ttl if not suggested by the sso connector" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(None)
      when(ssoDomainsConnectorMock.getDomains()(any[HeaderCarrier])).thenReturn(Future.successful(domainsFromRealSSO))
      when(domainsFromRealSSO.maxAge).thenReturn(60)
      cachedDomainsService.getDomains().futureValue

      verify(cacheMock).set("sso/domains", domainsFromRealSSO, Duration(60, SECONDS))
    }

    "do not cache failed calls the end point as we do not want temporary call failures to become long lasting failures" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(None)
      when(ssoDomainsConnectorMock.getDomains()(any[HeaderCarrier])).thenReturn(Future.failed(new Exception("")))

      cachedDomainsService.getDomains().futureValue

      verify(cacheMock).get[DomainsResponse]("sso/domains")
      verifyNoMoreInteractions(cacheMock)
    }

  }

}
