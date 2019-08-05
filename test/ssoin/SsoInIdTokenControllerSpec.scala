package ssoin

import connectors.{SsoConnector, SsoInSessionInfo}
import org.mockito.ArgumentMatchers.{eq => eqTo, _}
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.domains.ContinueUrlValidator
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SsoInIdTokenControllerSpec extends UnitSpec with ScalaFutures with MockitoSugar with WithFakeApplication {

  trait Setup {
    implicit val hc = HeaderCarrier()
    val ssoConnectorMock = mock[SsoConnector]
    val continueUrlValidatorMock = mock[ContinueUrlValidator]

    when(continueUrlValidatorMock.isRelativeOrAbsoluteWhiteListed(any[ContinueUrl])(any[HeaderCarrier])).thenReturn(true)

    implicit val request = FakeRequest()
    val controller = new SsoInIdTokenController(ssoConnectorMock, continueUrlValidator = continueUrlValidatorMock)
  }

  "redeem" should {

    "return 404 if document not found for id" in new Setup {
      when(ssoConnectorMock.getSsoInSessionInfo(any())(any())).thenReturn(Future.successful(None))

      whenReady(controller.redeem("id", ContinueUrl("/relative-url"))(request)) { result =>
        status(result) shouldBe 404

        result.session.get(SessionKeys.sessionId) shouldBe None
        result.session.get(SessionKeys.userId) shouldBe None
        result.session.get(SessionKeys.authToken) shouldBe None

        verify(ssoConnectorMock).getSsoInSessionInfo(eqTo("id"))(any())
      }
    }

    "redirect to continue url with session when id found" in new Setup {
      when(ssoConnectorMock.getSsoInSessionInfo(any())(any())).thenReturn(Future.successful(Some(SsoInSessionInfo("bearerToken", "sessionId", "userId"))))

      whenReady(controller.redeem("id", ContinueUrl("/relative-url"))(request)) { result =>
        status(result) shouldBe 303

        result.header.headers(HeaderNames.LOCATION) shouldBe "/relative-url"
        result.session.get(SessionKeys.sessionId) shouldBe Some("sessionId")
        result.session.get(SessionKeys.userId) shouldBe Some("userId")
        result.session.get(SessionKeys.authToken) shouldBe Some("bearerToken")
        result.session.get(SessionKeys.lastRequestTimestamp) shouldBe defined
        result.session.get(SessionKeys.token) shouldBe defined
        result.session.get(SessionKeys.authProvider) shouldBe defined

        verify(ssoConnectorMock).getSsoInSessionInfo(eqTo("id"))(any())
      }
    }

  }

}
