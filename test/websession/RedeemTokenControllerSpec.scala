package websession

import java.net.URL

import auth.FrontendAuthConnector
import connectors.SsoConnector
import org.apache.commons.codec.binary.Base64
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.config.{SsoCrypto, SsoFrontendAuditConnector}
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.http.{HeaderNames => HmrcHeaderNames, Request => _, _}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.connectors.domain.{Accounts, Authority, ConfidenceLevel, CredentialStrength}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import websession.redeem.RedeemTokenController

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait RedeemTokenControllerBaseSpec extends UnitSpec with WithFakeApplication with ScalaFutures with MockitoSugar {

  trait Setup {
    val ssoFeHost = "ssoFeHost"
    val continueUrl = ContinueUrl("/somewhere")
    val absoluteUrlPath = "http://domain:1234/somewhere"
    private val encryptedMissingTokenId = "encryptedMissingTokenId"
    val encodedMissingToken = Base64.encodeBase64String(encryptedMissingTokenId.getBytes("UTF-8"))

    val bearerToken = "bearer token=="
    val sessionId = "session-id"
    val userId = "/auth/oid/1234"

    implicit val request: Request[AnyContent] = FakeRequest()

    val ssoConnectorMock = mock[SsoConnector]
    val ssoCryptoMock = mock[SsoCrypto]
    val ssoFrontendAuditConnectorMock = mock[SsoFrontendAuditConnector]
    val mockAuthConnector = mock[FrontendAuthConnector]

    val redeemTokenController = new RedeemTokenController(
      ssoConnector = ssoConnectorMock,
      ssoCrypto    = ssoCryptoMock,
      ssoFrontendAuditConnectorMock,
      mockAuthConnector
    )

    val authority = Authority(
      uri                  = userId,
      accounts             = Accounts(),
      loggedInAt           = None,
      previouslyLoggedInAt = None,
      credentialStrength   = CredentialStrength.Strong,
      confidenceLevel      = ConfidenceLevel.L200,
      userDetailsLink      = None,
      enrolments           = None,
      ids                  = None,
      legacyOid            = userId
    )
  }

}

class RedeemTokenControllerSpec extends RedeemTokenControllerBaseSpec {

  "redeem" should {

    "return 303 when the token is valid" in new Setup {
      val validToken = "http://sso.service/tokenId/1234"
      val base64encodedEncryptedCorrectToken = Base64.encodeBase64String("encryptedToken".getBytes("UTF-8"))
      val encryptedCorrectToken = Crypted.apply("encryptedToken")

      when(ssoCryptoMock.decrypt(encryptedCorrectToken)).thenReturn(PlainText(validToken))
      when(mockAuthConnector.getAuthority(eqTo(userId))(any(), any())).thenReturn(Future.successful(Some(authority)))
      when(mockAuthConnector.getUserDetails[String](any())(any(), any(), any())).thenReturn(Future.successful("api"))

      val apiToken = ApiToken(sessionId   = sessionId, bearerToken = "a-bearer-token", continueUrl = continueUrl.url, userId = userId)
      when(ssoConnectorMock.getTokenDetails(eqTo(new URL(validToken)))(any[HeaderCarrier])).thenReturn(apiToken)

      val result = await(redeemTokenController.redeem(base64encodedEncryptedCorrectToken)(request))

      status(result) shouldBe 303
      result.header.headers("Location") shouldBe continueUrl.url
      result.session.get(SessionKeys.sessionId) shouldBe Some(sessionId)
      result.session.get(SessionKeys.userId) shouldBe Some(userId)
      result.session.get(SessionKeys.token) should not be None
      result.session.get(SessionKeys.authProvider) shouldBe Some("api")

      val auditEventCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(ssoFrontendAuditConnectorMock).sendEvent(auditEventCaptor.capture())(any[HeaderCarrier], any[ExecutionContext])

      val auditEvent = auditEventCaptor.getValue
      auditEvent.auditType shouldBe "api-sso-token-redeemed"
      auditEvent.detail.get("continueUrl") shouldBe Some(continueUrl.url)
    }

    """set the auth provider session attribute to "GGW" when the user SSOs using a GG account""" in new Setup {
      val validToken = "http://sso.service/tokenId/1234"
      val base64encodedEncryptedCorrectToken = Base64.encodeBase64String("encryptedToken".getBytes("UTF-8"))
      val encryptedCorrectToken = Crypted.apply("encryptedToken")

      when(ssoCryptoMock.decrypt(encryptedCorrectToken)).thenReturn(PlainText(validToken))
      when(mockAuthConnector.getAuthority(eqTo(userId))(any(), any())).thenReturn(Future.successful(Some(authority)))
      when(mockAuthConnector.getUserDetails[String](any())(any(), any(), any())).thenReturn(Future.successful("GovernmentGateway"))

      val apiToken = ApiToken(sessionId   = sessionId, bearerToken = "a-bearer-token", continueUrl = continueUrl.url, userId = userId)
      when(ssoConnectorMock.getTokenDetails(eqTo(new URL(validToken)))(any[HeaderCarrier])).thenReturn(apiToken)

      val result = await(redeemTokenController.redeem(base64encodedEncryptedCorrectToken)(request))

      status(result) shouldBe 303
      result.session.get(SessionKeys.authProvider) shouldBe Some("GGW")
    }

    """set the auth provider session attribute to "IDA" when the user SSOs using a Verify account""" in new Setup {
      val validToken = "http://sso.service/tokenId/1234"
      val base64encodedEncryptedCorrectToken = Base64.encodeBase64String("encryptedToken".getBytes("UTF-8"))
      val encryptedCorrectToken = Crypted.apply("encryptedToken")

      when(ssoCryptoMock.decrypt(encryptedCorrectToken)).thenReturn(PlainText(validToken))
      when(mockAuthConnector.getAuthority(eqTo(userId))(any(), any())).thenReturn(Future.successful(Some(authority)))
      when(mockAuthConnector.getUserDetails[String](any())(any(), any(), any())).thenReturn(Future.successful("Verify"))

      val apiToken = ApiToken(sessionId   = sessionId, bearerToken = "a-bearer-token", continueUrl = continueUrl.url, userId = userId)
      when(ssoConnectorMock.getTokenDetails(eqTo(new URL(validToken)))(any[HeaderCarrier])).thenReturn(apiToken)

      val result = await(redeemTokenController.redeem(base64encodedEncryptedCorrectToken)(request))

      status(result) shouldBe 303
      result.session.get(SessionKeys.authProvider) shouldBe Some("IDA")
    }

    "return 404 for invalid token and audit it" in new Setup {
      val unknownToken = "http://sso.service/tokenId/unknown"
      val encryptedUnknownToken = Crypted.apply("encryptedUnknownToken")
      when(ssoCryptoMock.decrypt(encryptedUnknownToken)).thenReturn(PlainText(unknownToken))

      when(ssoConnectorMock.getTokenDetails(eqTo(new URL(unknownToken)))(any[HeaderCarrier])).thenReturn(Future.failed(new NotFoundException("token not found")))

      val result = redeemTokenController.redeem(encodedMissingToken)(request)

      status(result) shouldBe 404

      val auditEventCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(ssoFrontendAuditConnectorMock).sendEvent(auditEventCaptor.capture())(any[HeaderCarrier], any[ExecutionContext])

      val auditEvent = auditEventCaptor.getValue
      auditEvent.auditType shouldBe "api-sso-token-hack-attempt"
      auditEvent.detail.get("continueUrl") shouldBe Some("-")
    }

    "return 404 for when decryption of the token fails" in new Setup {
      when(ssoCryptoMock.decrypt(any[Crypted])).thenThrow(new RuntimeException("failed to decrypt"))

      val result = redeemTokenController.redeem("garbage")(request)

      status(result) shouldBe 404

      val auditEventCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(ssoFrontendAuditConnectorMock).sendEvent(auditEventCaptor.capture())(any[HeaderCarrier], any[ExecutionContext])

      val auditEvent = auditEventCaptor.getValue
      auditEvent.auditType shouldBe "api-sso-token-hack-attempt"
      auditEvent.detail.get("continueUrl") shouldBe Some("-")
    }

  }

}
