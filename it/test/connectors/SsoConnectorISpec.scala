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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{get, okJson, post, stubFor}
import models.ApiToken
import play.api.libs.json.Json
import play.api.test.Injecting
import play.mvc.Http.HeaderNames
import support.WireMockSpec
import uk.gov.hmrc.http.HeaderCarrier

import java.net.URL
import scala.concurrent.Future

class SsoConnectorISpec extends WireMockSpec with ApiJsonFormats with Injecting {

  trait Setup {
    val affordanceUri = "/affordance-uri"
    val token:         ApiToken = ApiToken("mockBearerToken", "mockSessionID", "mockContinueURL", None)
    val ssnInfo:       SsoInSessionInfo = SsoInSessionInfo("bearerToken", "sessionID")
    val serviceBaseURL: String = s"http://${wiremockHost}:${wiremockPort}"

    val ssoConnector: SsoConnector = inject[SsoConnector]
  }

  "SsoConnector" should {

    "on calling createToken, POST request to sso createTokensURL, return url constructed from the response LOCATION header" in new Setup {
      stubFor(
        post("/sso/api-tokens")
          .willReturn(
            okJson(Json.obj("api-tokens" -> s"${serviceBaseURL}/mock-create-token-url").toString).withHeader(HeaderNames.LOCATION, s"${serviceBaseURL}/mock-create-token-response-url")
          )
      )

      val futureUrl: Future[URL] = ssoConnector.createToken(token)(HeaderCarrier())
      await(futureUrl) shouldBe new URL(s"${serviceBaseURL}/mock-create-token-response-url")

    }

    "on calling createToken, POST request to sso createTokensURL, throw exception when no url in response LOCATION header" in new Setup {
      stubFor(
        post("/sso/api-tokens")
          .willReturn(
            okJson(Json.obj("api-tokens" -> s"${serviceBaseURL}/mock-create-token-url").toString)
          )
      )

      a[RuntimeException] shouldBe thrownBy {
        await(ssoConnector.createToken(token)(HeaderCarrier()))
      }
    }

    "on calling getTokenDetails, makes GET request to given url to obtain ApiToken" in new Setup {
      val mockUrlString = s"${serviceBaseURL}/mock-token-detail-request-url"
      stubFor(
        get("/mock-token-detail-request-url")
          .willReturn(
            okJson(Json.toJson(token).toString)
          )
      )

      val apiToken: ApiToken = await(ssoConnector.getTokenDetails(new URL(mockUrlString))(HeaderCarrier()))
      apiToken shouldBe token
    }
  }
}
