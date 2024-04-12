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

package controllers

import base.BaseISpec
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.HeaderNames

class ApiControllerWireMockSpec extends BaseISpec {

  "api" should {
    "create an api sso token with random session-id" in new Setup {
      expectAuthorityRecordToBeFound()
      expectTokenToBeSuccessfullyCreated("/somewhere")

      val response: WSResponse = await(
        resourceRequest("/web-session")
          .withQueryStringParameters("continueUrl" -> "/somewhere")
          .addHttpHeaders(HeaderNames.xSessionId -> sessionId)
          .addHttpHeaders(HeaderNames.authorisation -> authToken)
          .withFollowRedirects(false)
          .get()
      )
      response.status shouldBe OK

      val deviceIdCookie1: String = response.cookie("mdtpdi").get.value
      deviceIdCookie1 should not be empty

      val redeemTokenUrl: String = (response.json \ "_links" \ "session").as[String]
      redeemTokenUrl should include("/sso/session?token=")
    }
  }

  trait Setup {
    val username = s"perf-sa-${System.currentTimeMillis()}"
    val sessionId = "session-id-123"
    val authToken = "Bearer something"
    val userId = "/auth/session/1"

    def expectAuthorityRecordToBeFound(): Unit = {
      stubFor(
        get("/auth/authority")
          .withHeader(HeaderNames.authorisation, equalTo(authToken))
          .willReturn(
            okJson(
              Json
                .obj(
                  "uri" -> userId
                )
                .toString
            )
          )
      )
    }

    def expectTokenToBeSuccessfullyCreated(continueUrl: String): Unit = {
      stubFor(
        post("/sso/api-tokens")
          // the session ID is randomly generated, so the request body can't directly be matched
          .withRequestBody(containing(s""""bearer-token":"$authToken""""))
          .withRequestBody(containing(""""session-id":"""))
          .withRequestBody(containing(s""""continue-url":"$continueUrl""""))
          .willReturn(ok.withHeader(LOCATION, "/api-token-location"))
      )
    }
  }

}
