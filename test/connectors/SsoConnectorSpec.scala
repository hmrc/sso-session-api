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

import config.AppConfig
import models.ApiToken
import play.api.cache.AsyncCacheApi
import play.api.libs.json.Json
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

class SsoConnectorSpec extends UnitSpec with ApiJsonFormats {

  trait Setup {
    val cache = mock[AsyncCacheApi]

    val affordanceUri = "/affordance-uri"
    val http = mock[HttpClient]
    val serviceBaseURL = "http://mockbaseurl:1234"
    val token = ApiToken("mockBearerToken", "mockSessionID", "mockContinueURL", None)
    val ssnInfo = SsoInSessionInfo("bearerToken", "sessionID")
    val mockAppConfig = mock[AppConfig]

    val ssoConnector = new SsoConnector(cache, http, mockAppConfig)(ExecutionContext.global)
  }

  "SsoConnector" should {

    "return affordance URI using given json reader" in new Setup {

      when(cache.get[HttpResponse](any)(any)).thenReturn(Future.successful(None))
      when(mockAppConfig.ssoUrl).thenReturn("http://mockbaseurl:1234")

      when(http.GET[Either[UpstreamErrorResponse, HttpResponse]](any)(any, any, any)).thenReturn(Future.successful(
        Right(HttpResponse(
          200,
          json = Json.obj("api-tokens" -> "http://mock-create-token-url"),
          Map.empty,
        ))
      ))

      val url = await(ssoConnector.getRootAffordance(_ => affordanceUri)(HeaderCarrier()))
      url shouldBe new URL(serviceBaseURL + "/affordance-uri")
    }

    "on calling createToken, POST request to sso createTokensURL, return url constructed from the response LOCATION header" in new Setup {
      when(cache.get[HttpResponse](any)(any)).thenReturn(Future.successful(None))
      when(mockAppConfig.ssoUrl).thenReturn("http://mockbaseurl:1234")

      when(http.GET[Either[UpstreamErrorResponse, HttpResponse]](any)(any, any, any)).thenReturn(Future.successful(
        Right(HttpResponse(
          200,
          json = Json.obj("api-tokens" -> "http://mock-create-token-url"),
          Map.empty,
        ))
      ))

      when(http.POST[ApiToken, Either[UpstreamErrorResponse, HttpResponse]](any, any, any)(any, any, any, any)).thenReturn(
        Future.successful(Right(HttpResponse(
          200,
          json = Json.obj("api-tokens" -> "http://mock-create-token-url"),
          Map(HeaderNames.LOCATION -> Seq("http://mock-create-token-response-url")),
        )))
      )
      val futureUrl = ssoConnector.createToken(token)(HeaderCarrier())
      await(futureUrl) shouldBe new URL("http://mock-create-token-response-url")

    }

    "on calling createToken, POST request to sso createTokensURL, throw exception when no url in response LOCATION header" in new Setup {
      when(cache.get[HttpResponse](any)(any)).thenReturn(Future.successful(None))
      when(mockAppConfig.ssoUrl).thenReturn("http://mockbaseurl:1234")

      when(http.GET[Either[UpstreamErrorResponse, HttpResponse]](any)(any, any, any)).thenReturn(
        Future.successful(Right(HttpResponse(
          200,
          json = Json.obj("api-tokens" -> "http://mock-create-token-url"),
          Map.empty,
        )))
      )

      when(http.POST[ApiToken, Either[UpstreamErrorResponse, HttpResponse]](any, any, any)(any, any, any, any)).thenReturn(
        Future.successful(Right(HttpResponse(
          200,
          json = Json.obj("api-tokens" -> "http://mock-create-token-url"),
          Map.empty,
        )))
      )

      a[RuntimeException] shouldBe thrownBy {
        await(ssoConnector.createToken(token)(HeaderCarrier()))
      }
    }

    "on calling getTokenDetails, makes GET request to given url to obtain ApiToken" in new Setup {
      val mockUrlString = "http://mock-token-detail-request-url"
      when(http.GET[ApiToken](eqTo(mockUrlString))(any, any, any)).thenReturn(Future.successful(
        token
      ))
      val apiToken = await(ssoConnector.getTokenDetails(new URL(mockUrlString))(HeaderCarrier()))
      apiToken shouldBe token
    }
  }
}
