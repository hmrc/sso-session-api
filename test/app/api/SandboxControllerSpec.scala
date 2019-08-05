package app.api

import api.SandboxController
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.domains.{ContinueUrlValidator, WhiteListService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class SandboxControllerSpec extends UnitSpec with ScalaFutures with WithFakeApplication with MockitoSugar {
  val whiteListService = mock[WhiteListService]
  def fakeRequest = FakeRequest()

  object succeedingValidator extends ContinueUrlValidator(whiteListService) {
    override def isRelativeOrAbsoluteWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = Future.successful(true)
  }

  object failingValidator extends ContinueUrlValidator(whiteListService) {
    override def isRelativeOrAbsoluteWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = Future.successful(false)
  }

  "SandboxController" should {
    "Return a 200 and the correct JSON for a GET" in {
      val continueUrl = mock[ContinueUrl]
      val controller = new SandboxController(succeedingValidator)
      val result = controller.create(continueUrl)(fakeRequest)

      status(result) shouldBe OK
      contentAsJson(result) \ "_links" \ "session" shouldBe JsDefined(JsString(s"http://schema.org"))
    }

    "Redirect if the ContinueURL is not valid" in {
      val continueUrl = mock[ContinueUrl]
      val controller = new SandboxController(failingValidator)
      val result = controller.create(continueUrl)(fakeRequest)

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "Invalid Continue URL"
    }
  }

}
