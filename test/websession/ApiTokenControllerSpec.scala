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

package websession

import java.net.URL

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import auth.{AuthResponse, FrontendAuthConnector}
import config._
import connectors.SsoConnector
import domains.ContinueUrlValidator
import org.apache.commons.codec.binary.Base64
import org.mockito.ArgumentCaptor
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HeaderNames => HmrcHeaderNames, _}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.binders.ContinueUrl
import websession.create.ApiTokenController
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class ApiTokenControllerSpec extends UnitSpec with ScalaFutures with GuiceOneAppPerSuite {

  trait Setup {
    val ssoFeHost = "ssoFeHost"
    val continueUrl = ContinueUrl("/somewhere")
    val absoluteUrlPath = "http://domain:1234/somewhere"
    private val correctTokenId = new URL("http://sso.service/tokenId/1234")
    private val missingTokenId = new URL("http://sso.service/tokenId/2345")
    private val encryptedCorrectTokenId = "encryptedTokenId"
    val encodedCorrectToken = Base64.encodeBase64String(encryptedCorrectTokenId.getBytes("UTF-8"))
    private val encryptedMissingTokenId = "encryptedMissingTokenId"
    val encodedMissingToken = Base64.encodeBase64String(encryptedMissingTokenId.getBytes("UTF-8"))

    val bearerToken = "bearer token=="
    val sessionId = "session-id"
    val userId = "/auth/oid/1234"

    implicit val request = FakeRequest()
    val createRequestWithSessionIdAndBearerToken = FakeRequest().withHeaders(
      HmrcHeaderNames.xSessionId -> sessionId,
      HmrcHeaderNames.authorisation -> bearerToken
    )

    val mockAppConfig = mock[AppConfig]

    val mockSsoConnector = mock[SsoConnector]
    val mockAuthConnector = mock[FrontendAuthConnector]
    val mockAuditConnector = mock[AuditConnector]
    val mockContinueUrlValidator = mock[ContinueUrlValidator]
    val messagesControllerComponents: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

    val apiTokenController = new ApiTokenController(
      ssoConnector         = mockSsoConnector,
      authConnector        = mockAuthConnector,
      auditConnector       = mockAuditConnector,
      frontendAppConfig    = mockAppConfig,
      continueUrlValidator = mockContinueUrlValidator,
      messagesControllerComponents
    )
  }

  "create" should {

    implicit lazy val system = ActorSystem("jsonBodyMaterialing")
    implicit val materializer = ActorMaterializer()

    "respond with 200 and encrypted token in message" in new Setup {
      when(mockContinueUrlValidator.isRelativeOrAbsoluteWhiteListed(any[ContinueUrl])(any[HeaderCarrier])).thenReturn(Future.successful(true))
      when(mockAppConfig.ssoFeHost).thenReturn("ssoFeHost")

      val tokenUrl = new URL("http://sso.service/tokenId/1234")
      when(mockAuthConnector.getAuthUri()(any, any)).thenReturn(Future.successful(Some(AuthResponse("testing"))))

      when(mockSsoConnector.createToken(any[ApiToken])(any[HeaderCarrier])).thenReturn(Future.successful(tokenUrl))

      val cryptedTokenUrl = mock[Crypted]
      when(mockAppConfig.encrypt(eqTo(PlainText(tokenUrl.toString)))).thenReturn(cryptedTokenUrl)
      when(cryptedTokenUrl.toBase64).thenReturn(encodedCorrectToken.getBytes())

      val result = apiTokenController.create(continueUrl)(createRequestWithSessionIdAndBearerToken)

      status(result) shouldBe 200

      val sessionLink = (contentAsJson(result) \ "_links" \ "session").as[String]
      sessionLink shouldBe (ssoFeHost + s"/sso/session?token=$encodedCorrectToken")

      val auditEventCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(mockAuditConnector).sendEvent(auditEventCaptor.capture())(any[HeaderCarrier], any[ExecutionContext])
      auditEventCaptor.getAllValues.asScala.exists(e => e.auditType == "api-sso-token-created" && e.detail.get("continueUrl").contains(continueUrl.url)) shouldBe true
    }

    "respond with 401 if the user is not logged in" in new Setup {
      when(mockAuthConnector.getAuthUri()(any, any)).thenReturn(Future.successful(None))

      val result = apiTokenController.create(continueUrl)(createRequestWithSessionIdAndBearerToken)
      status(result) shouldBe 401
    }

    "respond with 200 if no session-id provided" in new Setup {
      when(mockAuthConnector.getAuthUri()(any, any)).thenReturn(Future.successful(Some(AuthResponse("testing"))))
      when(mockContinueUrlValidator.isRelativeOrAbsoluteWhiteListed(any[ContinueUrl])(any[HeaderCarrier])).thenReturn(Future.successful(true))
      when(mockAuditConnector.sendEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Success))
      when(mockAppConfig.ssoFeHost).thenReturn("ssoFeHost")

      val tokenUrl = new URL("http://sso.service/tokenId/1234")
      when(mockSsoConnector.createToken(any[ApiToken])(any[HeaderCarrier])).thenReturn(Future.successful(tokenUrl))

      val cryptedTokenUrl = mock[Crypted]
      when(mockAppConfig.encrypt(eqTo(PlainText(tokenUrl.toString)))).thenReturn(cryptedTokenUrl)
      when(cryptedTokenUrl.toBase64).thenReturn(encodedCorrectToken.getBytes())

      val result = apiTokenController.create(continueUrl)(request.withHeaders(HmrcHeaderNames.authorisation -> bearerToken))

      status(result) shouldBe 200
    }
  }

}
