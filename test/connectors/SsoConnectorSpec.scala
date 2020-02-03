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

package connectors

import java.net.URL

import org.scalatest.concurrent.ScalaFutures
import play.api.cache.CacheApi
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import websession.ApiToken

import scala.concurrent.{ExecutionContext, Future}

class SsoConnectorSpec extends UnitSpec with ScalaFutures with ApiJsonFormats {

  trait Setup {

    implicit val hc = mock[HeaderCarrier]
    implicit val ec = scala.concurrent.ExecutionContext.global
    implicit val request = FakeRequest()

    val cache = mock[CacheApi]

    val affordanceUri = "/affordance-uri"
    val http = mock[HttpClient]
    val serviceBaseURL = "http://mockbaseurl:1234"
    val token = ApiToken("mockBearerToken", "mockSessionID", "mockContinueURL", "mockUserID")
    val ssnInfo = SsoInSessionInfo("bearerToken", "sessionID", "userID")

    class TestSsoConnector extends SsoConnector(cache, http) {

      override def baseUrl(serviceName: String): String = serviceBaseURL

      override implicit def getExecutionContext(implicit loggingDetails: LoggingDetails): ExecutionContext = scala.concurrent.ExecutionContext.global
    }

    val ssoConnector = new TestSsoConnector()

  }

  "SsoConnector" should {

    "return affordance URI using given json reader" in new Setup {

      when(cache.get[HttpResponse](any)(any)).thenReturn(None)

      when(http.GET[HttpResponse](any)(any, any, any)).thenReturn(Future.successful(
        HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](),
          Option[String]("response string")
        )
      ))
      val affordanceURL = ssoConnector.getRootAffordance(jsValue => affordanceUri)
      whenReady(affordanceURL) { url => url shouldBe new URL(serviceBaseURL + "/affordance-uri") }
    }

    "on calling createToken, POST request to sso createTokensURL, return url constructed from the response LOCATION header" in new Setup {
      when(cache.get[HttpResponse](any)(any)).thenReturn(None)

      when(http.GET[HttpResponse](any)(any, any, any)).thenReturn(Future.successful(
        HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](),
          Option[String]("response string")
        )
      ))

      when(http.POST[ApiToken, HttpResponse](any, any, any)(any, any, any, any)).thenReturn(
        Future.successful(HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](HeaderNames.LOCATION -> Seq("http://mock-create-token-response-url")),
          Option[String]("")
        ))
      )
      val futureUrl = ssoConnector.createToken(token)
      await(futureUrl) shouldBe new URL("http://mock-create-token-response-url")

    }

    "on calling createToken, POST request to sso createTokensURL, throw exception when no url in response LOCATION header" in new Setup {
      when(cache.get[HttpResponse](any)(any)).thenReturn(None)
      when(http.GET[HttpResponse](any)(any, any, any)).thenReturn(Future.successful((HttpResponse(
        200,
        Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
        Map[String, Seq[String]](),
        Option[String]("response string")
      ))
      ))

      when(http.POST[ApiToken, HttpResponse](any, any, any)(any, any, any, any)).thenReturn(
        Future.successful(HttpResponse(
          200,
          Option[JsValue](Json.parse("{\"api-tokens\":\"http://mock-create-token-url\"}")),
          Map[String, Seq[String]](),
          Option[String]("")
        ))
      )

      val futureUrl = ssoConnector.createToken(token)
      futureUrl onFailure {
        case t => t.isInstanceOf[RuntimeException] shouldBe true
      }
      futureUrl.onSuccess({
        case value => fail("Should throw RuntimeException")
      })

    }

    "on calling getTokenDetails, makes GET request to given url to obtain ApiToken" in new Setup {
      val mockUrlString = "http://mock-token-detail-request-url"
      when(http.GET[ApiToken](matches(mockUrlString))(any, any, any)).thenReturn(Future.successful(
        token
      ))
      val fApiToken = ssoConnector.getTokenDetails(new URL(mockUrlString))
      whenReady(fApiToken) { token => token shouldEqual token }
    }
  }
}
