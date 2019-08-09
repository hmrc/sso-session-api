/*
 * Copyright 2019 HM Revenue & Customs
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
import auth.FrontendAuthConnector
import connectors.SsoConnector
import org.apache.commons.codec.binary.Base64
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.config._
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.domains.ContinueUrlValidator
import uk.gov.hmrc.http.{HeaderNames => HmrcHeaderNames, _}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority
import uk.gov.hmrc.play.test.UnitSpec
import websession.create.ApiTokenController

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class ApiTokenControllerSpec extends UnitSpec with ScalaFutures with MockitoSugar {

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

    val appConfigMock = mock[FrontendAppConfig]
    when(appConfigMock.ssoFeHost).thenReturn("ssoFeHost")

    val ssoConnectorMock = mock[SsoConnector]
    val ssoCryptoMock = mock[SsoCrypto]
    val authConnectorMock = mock[FrontendAuthConnector]
    val auditConnectorMock = mock[SsoFrontendAuditConnector]
    val continueUrlValidatorMock = mock[ContinueUrlValidator]

    when(continueUrlValidatorMock.isRelativeOrAbsoluteWhiteListed(any[ContinueUrl])(any[HeaderCarrier])).thenReturn(true)

    val apiTokenController = new ApiTokenController(
      ssoConnector         = ssoConnectorMock,
      ssoCrypto            = ssoCryptoMock,
      authConnector        = authConnectorMock,
      auditConnector       = auditConnectorMock,
      frontendAppConfig    = appConfigMock,
      continueUrlValidator = continueUrlValidatorMock
    )
  }

  "create" should {

    implicit lazy val system = ActorSystem("jsonBodyMaterialing")
    implicit val materializer = ActorMaterializer()

    "respond with 200 and encrypted token in message" in new Setup {
      val signedInUserAuthority = mock[Authority]
      when(authConnectorMock.currentAuthority(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(signedInUserAuthority)))

      val tokenUrl = new URL("http://sso.service/tokenId/1234")
      when(ssoConnectorMock.createToken(any[ApiToken])(any[HeaderCarrier])).thenReturn(Future.successful(tokenUrl))

      val cryptedTokenUrl = mock[Crypted]
      when(ssoCryptoMock.encrypt(eqTo(PlainText(tokenUrl.toString)))).thenReturn(cryptedTokenUrl)
      when(cryptedTokenUrl.toBase64).thenReturn(encodedCorrectToken.getBytes())

      val result = apiTokenController.create(continueUrl)(createRequestWithSessionIdAndBearerToken)

      status(result) shouldBe 200

      val sessionLink = jsonBodyOf(result.futureValue) \ "_links" \ "session"
      sessionLink.as[String] shouldBe (ssoFeHost + s"/sso/session?token=$encodedCorrectToken")

      val auditEventCaptor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])
      verify(auditConnectorMock).sendEvent(auditEventCaptor.capture())(any[HeaderCarrier], any[ExecutionContext])
      auditEventCaptor.getAllValues.asScala.find(e => e.auditType == "api-sso-token-created" && e.detail.get("continueUrl") == Some(continueUrl.url)).nonEmpty shouldBe true
    }

    "respond with 401 if the user is not logged in" in new Setup {
      when(authConnectorMock.currentAuthority(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(None))

      val result = apiTokenController.create(continueUrl)(createRequestWithSessionIdAndBearerToken)

      status(result) shouldBe 401
    }

    "respond with 200 if no session-id provided" in new Setup {
      val signedInUserAuthority = mock[Authority]
      when(authConnectorMock.currentAuthority(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Some(signedInUserAuthority)))

      val tokenUrl = new URL("http://sso.service/tokenId/1234")
      when(ssoConnectorMock.createToken(any[ApiToken])(any[HeaderCarrier])).thenReturn(Future.successful(tokenUrl))

      val cryptedTokenUrl = mock[Crypted]
      when(ssoCryptoMock.encrypt(eqTo(PlainText(tokenUrl.toString)))).thenReturn(cryptedTokenUrl)
      when(cryptedTokenUrl.toBase64).thenReturn(encodedCorrectToken.getBytes())

      val result = apiTokenController.create(continueUrl)(request.withHeaders(HmrcHeaderNames.authorisation -> bearerToken))

      status(result) shouldBe 200
    }
  }

}
