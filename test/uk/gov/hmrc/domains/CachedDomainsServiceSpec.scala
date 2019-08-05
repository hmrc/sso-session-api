package service

import connectors.SsoDomainsConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.cache.CacheApi
import uk.gov.hmrc.domains.{CachedDomainsService, DomainsResponse}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, SECONDS}

class CachedDomainsServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val ssoDomainsConnectorMock = mock[SsoDomainsConnector]
    val cacheMock = mock[CacheApi]

    val domainsFromRealSSO = mock[DomainsResponse]
    val ssoResponseWithSuggestedTTL = (domainsFromRealSSO, Some("max-age=123"))
    val ssoResponseWithNoTTL = (domainsFromRealSSO, None)
    val cachedDomains = mock[DomainsResponse]

    val cachedDomainsService = new CachedDomainsService(ssoDomainsConnectorMock, cacheMock)
  }

  "white list service" should {

    "delegate to the real connector which the cache misses" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(None)
      when(ssoDomainsConnectorMock.getDomains()(any[HeaderCarrier])).thenReturn(Future.successful(domainsFromRealSSO))
      when(domainsFromRealSSO.maxAge).thenReturn(123)
      cachedDomainsService.getDomains().futureValue shouldBe Some(domainsFromRealSSO)
    }

    "return the cached value if the cache is populated" in new Setup {
      when(cacheMock.get[DomainsResponse]("sso/domains")).thenReturn(Some(cachedDomains))
      when(ssoDomainsConnectorMock.getDomains()(any[HeaderCarrier])).thenReturn(Future.successful(domainsFromRealSSO))
      when(domainsFromRealSSO.maxAge).thenReturn(123)
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
