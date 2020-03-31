package controllers

import base.BaseISpec
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.http.SessionKeys

class ApiControllerISpec extends BaseISpec {

  trait Setup {
    val username = s"perf-sa-${System.currentTimeMillis()}"
    val sessionId = "session-id-123"
  }

  "api" should {

    "create an api sso token with random session-id" in new Setup {
      val (authToken, userId) = signInToGetTokenAndAuthUri(username)

      val response: WSResponse = createApiToken("/somewhere", authToken, sessionId)
      response.status shouldBe 200

      val deviceIdCookie1 = response.cookie("mdtpdi").get.value
      deviceIdCookie1 shouldNot be(empty)

      val redeemTokenUrl = (response.json \ "_links" \ "session").as[String]
      redeemTokenUrl should include("/sso/session?token=")

      val redeemResponse = redeemToken(redeemTokenUrl)
      redeemResponse.status shouldBe 303
      redeemResponse.header("Location") shouldBe Some("/somewhere")
      val cookie = decryptCookie(redeemResponse.cookie("mdtp").get.value)
      cookie should {
        contain(SessionKeys.authToken -> authToken) and
          contain(SessionKeys.userId -> userId) and
          contain key SessionKeys.lastRequestTimestamp
      }

      cookie.get(SessionKeys.sessionId) shouldNot be(empty)
      cookie should not contain (SessionKeys.sessionId -> sessionId)

      val deviceIdCookie2 = redeemResponse.cookie("mdtpdi").get.value
      deviceIdCookie2 shouldNot be(empty)
    }
  }
}
