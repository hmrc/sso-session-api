package controllers

import base.BaseISpec
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderNames

class ApiControllerWireMockSpec extends BaseISpec {

  "api" should {
    "create an api sso token with random session-id" in new Setup {
      expectAuthorityRecordToBeFound()
      expectTokenToBeSuccessfullyCreated("/somewhere")

      val response = await(resourceRequest("/web-session")
        .withQueryStringParameters("continueUrl" -> "/somewhere")
        .addHttpHeaders(HeaderNames.xSessionId -> sessionId)
        .addHttpHeaders(HeaderNames.authorisation -> authToken)
        .withFollowRedirects(false)
        .get()
      )
      response.status shouldBe OK

      val deviceIdCookie1 = response.cookie("mdtpdi").get.value
      deviceIdCookie1 should not be empty

      val redeemTokenUrl = (response.json \ "_links" \ "session").as[String]
      redeemTokenUrl should include("/sso/session?token=")
    }
  }

  trait Setup {
    val username = s"perf-sa-${System.currentTimeMillis()}"
    val sessionId = "session-id-123"
    val authToken = "Bearer something"
    val userId = "/auth/session/1"

    def expectAuthorityRecordToBeFound(): Unit = {
      stubFor(get("/auth/authority").withHeader(HeaderNames.authorisation, equalTo(authToken))
        .willReturn(okJson(Json.obj(
          "uri" -> userId
        ).toString)))
    }

    def expectTokenToBeSuccessfullyCreated(continueUrl: String): Unit = {
      stubFor(post("/sso/api-tokens")
        // the session ID is randomly generated, so the request body can't directly be matched
        .withRequestBody(containing(s""""bearer-token":"$authToken""""))
        .withRequestBody(containing(""""session-id":"""))
        .withRequestBody(containing(s""""continue-url":"$continueUrl""""))
        .willReturn(ok.withHeader(LOCATION, "/api-token-location"))
      )
    }
  }

}
