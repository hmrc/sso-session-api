package uk.gov.hmrc.domains

import connectors.SsoDomainsConnector
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.http.HeaderNames
import play.api.libs.json.Json
import uk.gov.hmrc.config.WSHttp
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.{ExecutionContext, Future}

class SsoDomainsConnectorSpec extends UnitSpec with ScalaFutures with MockitoSugar with WithFakeApplication {

  trait Setup {

    implicit val hc = mock[HeaderCarrier]
    implicit val ec = scala.concurrent.ExecutionContext.global

    object Mock {
      val http = mock[WSHttp]
      val serviceBaseURL = "http://mockbaseurl:1234"
    }

    //so we can mock dependencies pulled in via other traits by overriding them
    class TestSsoDomainsConnector extends SsoDomainsConnector {
      override def http = Mock.http
      override def baseUrl(serviceName: String): String = Mock.serviceBaseURL
      override implicit def getExecutionContext(implicit loggingDetails: LoggingDetails): ExecutionContext = scala.concurrent.ExecutionContext.global
    }
    val ssoDomainsConnector = new TestSsoDomainsConnector()

  }

  "SsoDomainsConnector" should {

    "return Future[DomainsResponse] and max-age value when getDomains() is called" in new Setup {
      val mockWhiteListedDomains = WhiteListedDomains(Set("domain1.com", "domain2.com"), Set("domain3.com", "domain4.com"))

      when(Mock.http.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(
        HttpResponse(
          200,
          Some(Json.format[WhiteListedDomains].writes(mockWhiteListedDomains)),
          Map[String, Seq[String]](HeaderNames.CACHE_CONTROL -> Seq("max-age=33")),
          Some(Json.format[WhiteListedDomains].writes(mockWhiteListedDomains).toString())
        )
      ))

      val fDomains = ssoDomainsConnector.getDomains()
      whenReady(fDomains) {
        case DomainsResponse(WhiteListedDomains(extDomains, intDomains), maxAge) => {
          extDomains.collectFirst[String] { case s => s } shouldBe Some("domain1.com")
          intDomains.collectFirst[String] { case s => s } shouldBe Some("domain3.com")
          maxAge shouldBe 33
        }
        case _ => fail("WhiteListedDomains expected")
      }
    }

    "return Future[DomainsResponse] and default max-age value when getDomains() is called where no max-age header in http response" in new Setup {
      val mockWhiteListedDomains = WhiteListedDomains(Set("domain1.com", "domain2.com"), Set("domain3.com", "domain4.com"))

      when(Mock.http.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(
        HttpResponse(
          200,
          Some(Json.format[WhiteListedDomains].writes(mockWhiteListedDomains)),
          Map[String, Seq[String]](),
          Some(Json.format[WhiteListedDomains].writes(mockWhiteListedDomains).toString())
        )
      ))

      val fDomains = ssoDomainsConnector.getDomains()
      whenReady(fDomains) {
        case DomainsResponse(WhiteListedDomains(extDomains, intDomains), maxAge) => {
          extDomains.collectFirst[String] { case s => s } shouldBe Some("domain1.com")
          intDomains.collectFirst[String] { case s => s } shouldBe Some("domain3.com")
          maxAge shouldBe 60
        }
        case _ => fail("WhiteListedDomains expected")
      }
    }

  }

}

