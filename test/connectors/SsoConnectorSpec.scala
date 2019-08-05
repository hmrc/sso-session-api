package connectors

import java.net.URL

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.cache.CacheApi
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.config.WSHttp
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import websession.ApiToken

import scala.concurrent.{ExecutionContext, Future}

class SsoConnectorSpec extends UnitSpec with ScalaFutures with MockitoSugar with WithFakeApplication with ApiJsonFormats {

  trait Setup {

    implicit val hc = mock[HeaderCarrier]
    implicit val ec = scala.concurrent.ExecutionContext.global
    implicit val request = FakeRequest()

    val cache = mock[CacheApi]

    object Mock {
      val affordanceUri = "/affordance-uri"
      val http = mock[WSHttp]
      val serviceBaseURL = "http://mockbaseurl:1234"
      val token = ApiToken("mockBearerToken", "mockSessionID", "mockContinueURL", "mockUserID")
      val ssnInfo = SsoInSessionInfo("bearerToken", "sessionID", "userID")
    }

    class TestSsoConnector extends SsoConnector(cache) {

      override def http = Mock.http
      override def baseUrl(serviceName: String): String = Mock.serviceBaseURL
      override implicit def getExecutionContext(implicit loggingDetails: LoggingDetails): ExecutionContext = scala.concurrent.ExecutionContext.global
    }
    val ssoConnector = new TestSsoConnector()

  }

  "SsoConnector" should {

    "return affordance URI using given json reader" in new Setup {

      when(cache.get[HttpResponse](any())(any())).thenReturn(None)

      when(Mock.http.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(
        HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](),
          Option[String]("response string")
        )
      ))
      val affordanceURL = ssoConnector.getRootAffordance(jsValue => Mock.affordanceUri)
      whenReady(affordanceURL) { url => url shouldBe new URL(Mock.serviceBaseURL + "/affordance-uri") }
    }

    "on calling createToken, POST request to sso createTokensURL, return url constructed from the response LOCATION header" in new Setup {
      when(cache.get[HttpResponse](any())(any())).thenReturn(None)

      when(Mock.http.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(
        HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](),
          Option[String]("response string")
        )
      ))

      when(Mock.http.POST[ApiToken, HttpResponse](any(), any(), any())(any(), any(), any(), any())).thenReturn(
        HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](HeaderNames.LOCATION -> Seq("http://mock-create-token-response-url")),
          Option[String]("")
        )
      )
      val futureUrl = ssoConnector.createToken(Mock.token)
      whenReady(futureUrl) { url => url shouldBe new URL("http://mock-create-token-response-url") }

    }

    "on calling createToken, POST request to sso createTokensURL, throw exception when no url in response LOCATION header" in new Setup {
      when(cache.get[HttpResponse](any())(any())).thenReturn(None)
      when(Mock.http.GET[HttpResponse](any())(any(), any(), any())).thenReturn(Future.successful(
        HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](),
          Option[String]("response string")
        )
      ))

      when(Mock.http.POST[ApiToken, HttpResponse](any(), any(), any())(any(), any(), any(), any())).thenReturn(
        HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](),
          Option[String]("")
        )
      )

      val futureUrl = ssoConnector.createToken(Mock.token)
      futureUrl onFailure {
        case t => t.isInstanceOf[RuntimeException] shouldBe true
      }
      futureUrl.onSuccess({
        case value => fail("Should throw RuntimeException")
      })

    }

    "on calling getTokenDetails, makes GET request to given url to obtain ApiToken" in new Setup {
      val mockUrlString = "http://mock-token-detail-request-url"
      when(Mock.http.GET[ApiToken](matches(mockUrlString))(any(), any(), any())).thenReturn(Future.successful(
        Mock.token
      ))
      val fApiToken = ssoConnector.getTokenDetails(new URL(mockUrlString))
      whenReady(fApiToken) { token => token shouldEqual Mock.token }
    }

    "on calling getSsoInSessionInfo, makes GET request for SSO session in information using the given id, returns Future[Some[SsoInSessionInfo]]" in new Setup {
      val mockId = "1234"
      val mockUrlString = Mock.serviceBaseURL + "/sso/ssoin/sessionInfo/" + mockId
      when(Mock.http.GET[SsoInSessionInfo](matches(mockUrlString))(any(), any(), any())).thenReturn(Future.successful(
        Mock.ssnInfo
      ))
      val foInfo = ssoConnector.getSsoInSessionInfo(mockId)
      whenReady(foInfo) {
        case Some(info) => { info shouldEqual Mock.ssnInfo }
        case _          => { fail("expected Some SsoInSessionInfo to be returned") }
      }
    }

    "on calling getSsoInSessionInfo, makes GET request for SSO session in information using the given id, returns None on NotFoundException" in new Setup {
      val mockId = "1234"
      val mockUrlString = Mock.serviceBaseURL + "/sso/ssoin/sessionInfo/" + mockId
      when(Mock.http.GET[SsoInSessionInfo](matches(mockUrlString))(any(), any(), any())).thenReturn(Future.failed(new NotFoundException("")))
      val foInfo = ssoConnector.getSsoInSessionInfo(mockId)
      whenReady(foInfo) {
        case Some(info) => { fail("expected None to be returned due to NotFoundException") }
        case None       => {}
        case _          => { fail("expected None to be returned due to NotFoundException") }
      }
    }

  }

}
