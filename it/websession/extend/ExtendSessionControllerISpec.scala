package websession.extend

import java.lang.System.currentTimeMillis

import base.BaseISpec
import uk.gov.hmrc.http.SessionKeys

class ExtendSessionControllerISpec extends BaseISpec {

  trait Setup {
    val username = s"perf-sa-$currentTimeMillis"
    val sessionId = "session-id-123"
    val authToken = "someAuthToken123"
    val ggToken = "someGGToken123"
  }

  "/sso/:guid/transparent.png " should {

    "return image and not extend session if no mdtp session exists" in new Setup {
      val response = resourceRequest("/sso/123/transparent.png").get()
      response.status shouldBe 200
    }

    "return image and wipe the session if mdtp session has expired" in new Setup {
      val response = resourceRequest("/sso/123/transparent.png")
          .withSession(SessionKeys.lastRequestTimestamp -> "0", SessionKeys.token -> "123", SessionKeys.sessionId -> "456")
          .get()

      response.status shouldBe 200

      val session = decryptCookie(response.cookie("mdtp").get.value.get)
      session.get(SessionKeys.sessionId) shouldBe None
      session.get(SessionKeys.token) shouldBe None
    }

    "extend the lastRequestTimestamp of the session" in new Setup {
      val response = await(
        resourceRequest("/sso/123/transparent.png")
          .withSession(SessionKeys.lastRequestTimestamp -> currentTimeMillis.toString, SessionKeys.authToken -> authToken, SessionKeys.token -> ggToken, SessionKeys.sessionId -> "456")
          .get()
      )
      response.status shouldBe 200

      val session = decryptCookie(response.cookie("mdtp").get.value.get)
      val lastRequestTimestamp = session(SessionKeys.lastRequestTimestamp)
      session(SessionKeys.sessionId) shouldBe "456"
      session(SessionKeys.authToken) shouldBe authToken
      session(SessionKeys.token) shouldBe ggToken

      val secondExtendSessionResponse = resourceRequest("/sso/456/transparent.png")
          .withSession(SessionKeys.lastRequestTimestamp -> lastRequestTimestamp, SessionKeys.authToken -> authToken, SessionKeys.token -> ggToken, SessionKeys.sessionId -> "456")
          .get()

      secondExtendSessionResponse.status shouldBe 200

      val updatedSession = decryptCookie(secondExtendSessionResponse.cookie("mdtp").get.value.get)
      val lastRequestTimestamp2 = updatedSession(SessionKeys.lastRequestTimestamp)

      lastRequestTimestamp2 > lastRequestTimestamp shouldBe true
      updatedSession(SessionKeys.sessionId) shouldBe "456"
      updatedSession(SessionKeys.authToken) shouldBe authToken
      updatedSession(SessionKeys.token) shouldBe ggToken
    }
  }
}
