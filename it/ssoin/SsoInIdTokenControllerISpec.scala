package ssoin

import base.BaseISpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.Json
import uk.gov.hmrc.gg.test.it.SessionCookieEncryptionSupport
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}
import uk.gov.hmrc.time.DateTimeUtils

class SsoInIdTokenControllerISpec extends BaseISpec with SessionCookieEncryptionSupport with GuiceOneServerPerSuite {

  "GET /ssoin/:id" should {

    "propagates 400 returned by sso" in {
      val result = await(wsClient.url(resourceUrl("/sso/ssoin/123?continueUrl=/account")).get())
      result.status shouldBe 400
      (result.json \ "error").as[String] shouldBe "Invalid Id format"
    }

    "creates session when valid id" in {
      val currentTimestamp = DateTimeUtils.now.getMillis
      val sessionId = "sessionId"

      val (authToken, authUri) = signInToGetTokenAndAuthUri(s"random-user-$currentTimestamp")

      val createTokenResponse = await(wsClient.url(getOpenIdConnectTokenIdResource("/openid-connect-idtoken")))
        .withHeaders(HeaderNames.authorisation -> authToken)
        .post(Json.obj("clientId" -> "test-client-id"))
      createTokenResponse.status shouldBe 201


      val tokenResponse = await(wsClient.url(getOpenIdConnectTokenIdResource(createTokenResponse.header("Location").get)).get())
      tokenResponse.status shouldBe 200

      val tokenJson = Json.parse(tokenResponse.body)

      val token = (tokenJson \ "id_token").as[String]

      val requestBody = Json.obj("id_token" -> token, "mdtpSessionId" -> sessionId)

      val createResponse = await(wsClient.url(getSsoResource("/sso/ssoin/sessionInfo"))
        .withHeaders(HeaderNames.authorisation -> authToken).post(requestBody))
      createResponse.status shouldBe 200

      val ssoFrontendAffordance = (createResponse.json \ "_links" \ "browser" \ "href").as[String]
      ssoFrontendAffordance should startWith("/sso/ssoin")

      val response = await(wsClient.url(resourceUrl(s"$ssoFrontendAffordance?continueUrl=/account")).withFollowRedirects(false).get())
      response.status shouldBe 303
      response.header("Location") shouldBe Some("/account")

      val session = sessionOf(response)

      session(SessionKeys.authToken) shouldBe authToken

      session(SessionKeys.sessionId) shouldBe sessionId
      session(SessionKeys.userId) shouldBe authUri
      session(SessionKeys.authProvider) shouldBe "GGW"
      session(SessionKeys.token) shouldBe "dummy"
      session(SessionKeys.lastRequestTimestamp).toLong should be >= currentTimestamp
    }

  }

}
