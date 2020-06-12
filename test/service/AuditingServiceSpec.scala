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

package service

import audit.AuditingService
import org.joda.time.DateTimeUtils
import org.mockito.captor.ArgCaptor
import org.scalatest.BeforeAndAfterEach
import play.api.test.FakeRequest
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.time.{DateTimeUtils => HmrcDateTimeUtils}

import scala.concurrent.{ExecutionContext, Future}

class AuditingServiceSpec extends UnitSpec with BeforeAndAfterEach {

  "The auditing service" should {
    "send the correct event when a token is created" in new Setup {
      val redirectUrl = "/redirectUrl"

      await(service.sendTokenCreatedEvent(redirectUrl)(fakeRequestWithHeaders))

      val dataEventCaptor = ArgCaptor[DataEvent]
      verify(mockAuditConnector).sendEvent(dataEventCaptor.capture)(any, any)
      val dataEvent = dataEventCaptor.value

      dataEvent.auditSource shouldBe "sso-session-api"
      dataEvent.auditType shouldBe "api-sso-token-created"
      dataEvent.tags shouldBe Map(
        "clientIP" -> clientIp,
        "path" -> fakeRequestWithHeaders.path,
        HeaderNames.xSessionId -> sessionId,
        HeaderNames.akamaiReputation -> clientReputation,
        HeaderNames.xRequestId -> requestId,
        HeaderNames.deviceID -> deviceId,
        "clientPort" -> clientPort,
        "transactionName" -> "api-sso-token-created"
      )
      dataEvent.detail shouldBe Map(
        "continueUrl" -> redirectUrl,
        "session-id" -> sessionId,
        "user-id" -> userid,
        "bearer-token" -> authToken
      )
      dataEvent.generatedAt.toString shouldBe HmrcDateTimeUtils.now.toString
    }
  }

  trait Setup {
    val userIdentifier = "somebody"
    val clientIp = "192.168.0.1"
    val clientPort = "443"
    val clientReputation = "totally reputable"
    val requestId = "requestId"
    val deviceId = "deviceId"
    val sessionId = "sessionId"
    val authToken = "authToken"
    val userid = "userid"

    val fakeRequestWithHeaders = FakeRequest()
      .withHeaders(HeaderNames.trueClientIp -> clientIp)
      .withHeaders(HeaderNames.trueClientPort -> clientPort)
      .withHeaders(HeaderNames.akamaiReputation -> clientReputation)
      .withHeaders(HeaderNames.xRequestId -> requestId)
      .withHeaders(HeaderNames.deviceID -> deviceId)
      .withSession(SessionKeys.sessionId -> sessionId)
      .withSession(SessionKeys.authToken -> authToken)
      .withSession(SessionKeys.userId -> userid)

    val mockAuditConnector = mock[AuditConnector]
    when(mockAuditConnector.sendEvent(any)(any, any)).thenReturn(Future.successful(AuditResult.Disabled))

    val service = new AuditingService(mockAuditConnector)(ExecutionContext.global)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    DateTimeUtils.setCurrentMillisFixed(System.currentTimeMillis())
  }

  override protected def afterEach(): Unit = {
    super.afterEach()
    DateTimeUtils.setCurrentMillisSystem()
  }
}
