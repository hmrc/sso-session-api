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
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.*
import play.api.libs.json.Json
import play.api.libs.ws.{DefaultWSCookie, WSResponse}
import uk.gov.hmrc.http.HeaderNames

class ApiControllerWireMockSpec extends BaseISpec {

  override def extraConfig: Map[String, Any] = super.extraConfig + ("auditing.enabled" -> true)

  "api" should {
    "create an api sso token" in new Setup {
      expectAuthorityRecordToBeFound()
      expectTokenToBeSuccessfullyCreated("/somewhere")
      setupForCentralAuthorisation()
      expectMdtpInformationRetrievalToReturnNothing()
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

  "[GG-9130] add a sessionId if one is not provided in the request and we can't retrieve one from auth" in new Setup {
    expectAuthorityRecordToBeFound()
    expectTokenToBeSuccessfullyCreated("/somewhere")
    setupForCentralAuthorisation()
    expectMdtpInformationRetrievalToReturnNothing()
    val response: WSResponse = await(
      resourceRequest("/web-session")
        .withQueryStringParameters("continueUrl" -> "/somewhere")
        .addHttpHeaders(HeaderNames.authorisation -> authToken)
        .withFollowRedirects(false)
        .get()
    )
    response.status shouldBe OK

    val setSessionId = decryptCookie(response.cookie("mdtp").get.value).get("sessionId")

    setSessionId shouldBe defined
  }


  "[GG-9130] reuse the sessionId passed in the header, if there is no session in the mdtp cookie and if none can be retrieved from auth" in new Setup {
    val sessionIdPassedInHeader = "session-0cd77215-00a6-4f99-8f45-e6db9c79019c"
    expectAuthorityRecordToBeFound()
    expectTokenToBeSuccessfullyCreated("/somewhere")
    setupForCentralAuthorisation()
    expectMdtpInformationRetrievalToReturnNothing()
    val response: WSResponse = await(
      resourceRequest("/web-session")
        .withQueryStringParameters("continueUrl" -> "/somewhere")
        .addHttpHeaders(HeaderNames.xSessionId -> sessionIdPassedInHeader)
        .addHttpHeaders(HeaderNames.authorisation -> authToken)
        .withFollowRedirects(false)
        .get()
    )
    response.status shouldBe OK

    val setSessionId = decryptCookie(response.cookie("mdtp").get.value).get("sessionId")

    setSessionId shouldBe Some(sessionIdPassedInHeader)

    val expectedAuditJson =
      s"""{
         |  "auditType" : "api-sso-token-created",
         |  "detail" : {
         |    "session-id" : "$sessionIdPassedInHeader"
         |  }
         |}
         |""".stripMargin
    WireMock.verify(postRequestedFor(urlPathEqualTo("/write/audit")).withRequestBody(equalToJson(expectedAuditJson, /* ignoreArrayOrder */ false, /* ignoreExtraElements */ true)))
  }

  "[GG-9130] reuse the sessionId passed in the mdtp cookie, if none can be retrieved from auth" in new Setup {
    val sessionIdPassedInCookie = "session-00118f71-c825-47f6-a417-e4e649fb1175"
    expectAuthorityRecordToBeFound()
    expectTokenToBeSuccessfullyCreated("/somewhere")
    setupForCentralAuthorisation()
    expectMdtpInformationRetrievalToReturnNothing()
    val response: WSResponse = await(
      resourceRequest("/web-session")
        .withQueryStringParameters("continueUrl" -> "/somewhere")
        .addHttpHeaders(HeaderNames.authorisation -> authToken)
        .addCookies(DefaultWSCookie("mdtp", encryptCookie(Map("sessionId" -> sessionIdPassedInCookie))))
        .withFollowRedirects(false)
        .get()
    )
    response.status shouldBe OK

    val setSessionId = decryptCookie(response.cookie("mdtp").get.value).get("sessionId")

    setSessionId shouldBe Some(sessionIdPassedInCookie)

    val expectedAuditJson =
      s"""{
         |  "auditType" : "api-sso-token-created",
         |  "detail" : {
         |    "session-id" : "$sessionIdPassedInCookie"
         |  }
         |}
         |""".stripMargin
    WireMock.verify(postRequestedFor(urlPathEqualTo("/write/audit")).withRequestBody(equalToJson(expectedAuditJson, /* ignoreArrayOrder */ false, /* ignoreExtraElements */ true)))
  }

  "[GG-9130] use sessionId from auth, if one can be retrieved (even if no sessionId present in request)" in new Setup {
    val sessionIdRetrievedFromAuth = "session-210821e5-20e8-4060-81c8-fee076038997"
    expectAuthorityRecordToBeFound()
    expectTokenToBeSuccessfullyCreated("/somewhere")
    setupForCentralAuthorisation()
    expectMdtpInformationRetrievalToSucceedWithSessionId(sessionIdRetrievedFromAuth)
    val response: WSResponse = await(
      resourceRequest("/web-session")
        .withQueryStringParameters("continueUrl" -> "/somewhere")
        .addHttpHeaders(HeaderNames.authorisation -> authToken)
        .withFollowRedirects(false)
        .get()
    )
    response.status shouldBe OK

    val setSessionId = decryptCookie(response.cookie("mdtp").get.value).get("sessionId")

    setSessionId shouldBe Some(sessionIdRetrievedFromAuth)

    val expectedAuditJson =
      s"""{
         |  "auditType" : "api-sso-token-created",
         |  "detail" : {
         |    "session-id" : "$sessionIdRetrievedFromAuth"
         |  }
         |}
         |""".stripMargin
    WireMock.verify(postRequestedFor(urlPathEqualTo("/write/audit")).withRequestBody(equalToJson(expectedAuditJson, /* ignoreArrayOrder */ false, /* ignoreExtraElements */ true)))
  }

  "[GG-9130] use sessionId from auth in preference to sessionId(s) stored in request" in new Setup {
    val sessionIdPassedInHeader = "session-0cd77215-00a6-4f99-8f45-e6db9c79019c"
    val sessionIdPassedInCookie = "session-00118f71-c825-47f6-a417-e4e649fb1175"
    val sessionIdRetrievedFromAuth = "session-210821e5-20e8-4060-81c8-fee076038997"
    expectAuthorityRecordToBeFound()
    expectTokenToBeSuccessfullyCreated("/somewhere")
    setupForCentralAuthorisation()
    expectMdtpInformationRetrievalToSucceedWithSessionId(sessionIdRetrievedFromAuth)
    val response: WSResponse = await(
      resourceRequest("/web-session")
        .withQueryStringParameters("continueUrl" -> "/somewhere")
        .addHttpHeaders(HeaderNames.authorisation -> authToken)
        .addHttpHeaders(HeaderNames.xSessionId -> sessionIdPassedInHeader)
        .addCookies(DefaultWSCookie("mdtp", encryptCookie(Map("sessionId" -> sessionIdPassedInCookie))))
        .withFollowRedirects(false)
        .get()
    )
    response.status shouldBe OK

    val setSessionId = decryptCookie(response.cookie("mdtp").get.value).get("sessionId")

    setSessionId shouldBe Some(sessionIdRetrievedFromAuth)

    val expectedAuditJson =
      s"""{
         |  "auditType" : "api-sso-token-created",
         |  "detail" : {
         |    "session-id" : "$sessionIdRetrievedFromAuth"
         |  }
         |}
         |""".stripMargin
    WireMock.verify(postRequestedFor(urlPathEqualTo("/write/audit")).withRequestBody(equalToJson(expectedAuditJson, /* ignoreArrayOrder */ false, /* ignoreExtraElements */ true)))
  }

  trait Setup {
    val username: String = s"perf-sa-${System.currentTimeMillis()}"
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

    def expectMdtpInformationRetrievalToReturnNothing(): Unit = {
      stubFor(
        post("/auth/authorise")
          .withRequestBody(equalToJson("""{ "retrieve" : ["mdtpInformation"] }""", /* ignoreArrayOrder */ false, /* ignoreExtraElements */ true))
          .willReturn(
            okJson(
              Json
                .obj()
                .toString
            )
          )
      )
    }

    def expectMdtpInformationRetrievalToSucceedWithSessionId(sessionId: String): Unit = {
      stubFor(
        post("/auth/authorise")
          .withRequestBody(equalToJson("""{ "retrieve" : ["mdtpInformation"] }""", /* ignoreArrayOrder */ false, /* ignoreExtraElements */ true))
          .willReturn(
            okJson(
              Json
                .obj(
                  "mdtpInformation" -> Json.obj("deviceId" -> "myDeviceId", "sessionId" -> sessionId)
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

    def setupForCentralAuthorisation(): Unit = {
      stubFor(post("/centralised-authorisation-server/token").willReturn(aResponse().withStatus(204)))
    }
  }

}
