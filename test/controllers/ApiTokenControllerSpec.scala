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

package controllers

import java.net.URL

import audit.AuditingService
import config._
import connectors.{AuthResponse, FrontendAuthConnector, SsoConnector}
import org.apache.commons.codec.binary.Base64
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import services.ContinueUrlValidator
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HeaderNames => HmrcHeaderNames}
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, SafeRedirectUrl}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiTokenControllerSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  trait Setup {
    val ssoFeHost = "ssoFeHost"
    val continueUrl = RedirectUrl("/somewhere")
    val absoluteUrlPath = "http://domain:1234/somewhere"
    private val encryptedCorrectTokenId = "encryptedTokenId"
    val encodedCorrectToken = Base64.encodeBase64String(encryptedCorrectTokenId.getBytes("UTF-8"))
    private val encryptedMissingTokenId = "encryptedMissingTokenId"
    val encodedMissingToken = Base64.encodeBase64String(encryptedMissingTokenId.getBytes("UTF-8"))

    val bearerToken = "bearer token=="
    val sessionId = "session-id"
    val userId = "/auth/oid/1234"

    val createRequestWithSessionIdAndBearerToken = FakeRequest().withHeaders(
      HmrcHeaderNames.xSessionId -> sessionId,
      HmrcHeaderNames.authorisation -> bearerToken
    )

    val mockAppConfig = mock[AppConfig]
    val mockSsoConnector = mock[SsoConnector]
    val mockAuthConnector = mock[FrontendAuthConnector]
    val mockAuditingService = mock[AuditingService]
    val mockContinueUrlValidator = mock[ContinueUrlValidator]
    val messagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    val apiTokenController = new ApiTokenController(
      ssoConnector         = mockSsoConnector,
      authConnector        = mockAuthConnector,
      auditingService      = mockAuditingService,
      frontendAppConfig    = mockAppConfig,
      continueUrlValidator = mockContinueUrlValidator,
      messagesControllerComponents
    )
  }

  "create" should {

    "respond with 200 and encrypted token in message" in new Setup {
      when(mockContinueUrlValidator.getRelativeOrAbsoluteWhiteListed(any)(any)).thenReturn(Future.successful(Some(SafeRedirectUrl(continueUrl.unsafeValue))))
      when(mockAppConfig.ssoFeHost).thenReturn("ssoFeHost")

      val tokenUrl = new URL("http://sso.service/tokenId/1234")
      when(mockAuthConnector.getAuthUri()(any, any)).thenReturn(Future.successful(Some(AuthResponse("testing"))))

      when(mockSsoConnector.createToken(any)(any)).thenReturn(Future.successful(tokenUrl))

      val cryptedTokenUrl = mock[Crypted]
      when(mockAppConfig.encrypt(eqTo(PlainText(tokenUrl.toString)))).thenReturn(cryptedTokenUrl)
      when(cryptedTokenUrl.toBase64).thenReturn(encodedCorrectToken.getBytes())

      val result = apiTokenController.create(continueUrl)(createRequestWithSessionIdAndBearerToken)

      status(result) shouldBe 200

      val sessionLink = (contentAsJson(result) \ "_links" \ "session").as[String]
      sessionLink shouldBe (ssoFeHost + s"/sso/session?token=$encodedCorrectToken")

      verify(mockAuditingService).sendTokenCreatedEvent(eqTo(continueUrl.unsafeValue))(any)
    }

    "respond with 401 if the user is not logged in" in new Setup {
      when(mockAuthConnector.getAuthUri()(any, any)).thenReturn(Future.successful(None))

      val result = apiTokenController.create(continueUrl)(createRequestWithSessionIdAndBearerToken)
      status(result) shouldBe 401
    }

    "respond with 200 if no session-id provided" in new Setup {
      when(mockAuthConnector.getAuthUri()(any, any)).thenReturn(Future.successful(Some(AuthResponse("testing"))))
      when(mockContinueUrlValidator.getRelativeOrAbsoluteWhiteListed(any)(any)).thenReturn(Future.successful(Some(SafeRedirectUrl(continueUrl.unsafeValue))))
      when(mockAuditingService.sendTokenCreatedEvent(any)(any)).thenReturn(Future.unit)
      when(mockAppConfig.ssoFeHost).thenReturn("ssoFeHost")

      val tokenUrl = new URL("http://sso.service/tokenId/1234")
      when(mockSsoConnector.createToken(any)(any)).thenReturn(Future.successful(tokenUrl))

      val cryptedTokenUrl = mock[Crypted]
      when(mockAppConfig.encrypt(eqTo(PlainText(tokenUrl.toString)))).thenReturn(cryptedTokenUrl)
      when(cryptedTokenUrl.toBase64).thenReturn(encodedCorrectToken.getBytes())

      val result = apiTokenController.create(continueUrl)(FakeRequest().withHeaders(HmrcHeaderNames.authorisation -> bearerToken))

      status(result) shouldBe 200
    }
  }

}
