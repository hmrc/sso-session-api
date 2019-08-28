package base

import java.util.UUID

import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSResponse
import play.api.mvc.Session
import uk.gov.hmrc.config.FrontendGlobal
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, PlainText}
import uk.gov.hmrc.gg.test.it.{SessionCookieEncryptionSupport, SmIntegrationSpecBase}
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionKeys}
import uk.gov.hmrc.play.http.ws.WSRequest


trait BaseISpec extends SmIntegrationSpecBase with WSRequest with SessionCookieEncryptionSupport {

  override protected def extraConfig = Map(
    "Test.microservice.services.service-locator.enabled" -> false
  )

  implicit val hc = HeaderCarrier()

  def getAuthResource(path: String) = getExternalResourceUrl("auth", path)
  def getSsoResource(path: String) = getExternalResourceUrl("sso", path)
  def getOpenIdConnectTokenIdResource(resource: String) = s"http://$localhost:9841$resource"
  def getAuthApiResource(resource: String) = s"http://$localhost:8585$resource"

  def signInToGetTokenAndAuthUri(username: String) = {
    val request = Json.obj(
      "credId" -> "a-cred-id",
      "affinityGroup" -> "Individual",
      "confidenceLevel" -> 200,
      "credentialStrength" -> "strong",
      "enrolments" -> JsArray(),
      "mdtpInformation" -> Json.obj(
        "deviceId" -> "deviceId",
        "sessionId" -> "sessionId")
    )

    val exchangeResult = await(buildRequest(getAuthApiResource(s"/government-gateway/session/login")).post(request))
    val authToken = exchangeResult.header("Authorization").getOrElse("")
    val authUri = exchangeResult.header("Location").getOrElse("")

    exchangeResult.status shouldBe 201
    (authToken,authUri)
  }


  def createApiToken(continueUrl: String, authToken: String, sessionId: String = UUID.randomUUID().toString): WSResponse = {
    await(resourceRequest("/web-session")
      .withQueryString(("continueUrl", continueUrl))
      .withFollowRedirects(false)
      .withHeaders(HeaderNames.authorisation -> authToken, HeaderNames.xSessionId -> sessionId)
      .get())
  }

  def redeemToken(fullRedeemUrl: String) : WSResponse = {
    await(buildRequest(fullRedeemUrl).withSession(SessionKeys.lastRequestTimestamp -> System.currentTimeMillis.toString).withFollowRedirects(false).get())
  }

  def decryptCookie(mdtpSessionCookieValue: String): Map[String, String] = {
    val applicationCrypto = new ApplicationCrypto(FrontendGlobal.runModeConfiguration.underlying)

    applicationCrypto.SessionCookieCrypto.decrypt(Crypted(mdtpSessionCookieValue)) match {
      case PlainText(v) => Session.decode(v)
      case _ => Map.empty[String, String]
    }
  }

  override def applicableHeaders(url: String)(implicit hc: HeaderCarrier) = Nil

}
