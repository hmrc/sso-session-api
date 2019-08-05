package controllers

import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.domains.{ContinueUrlValidator, WhiteListService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContinueUrlValidatorSpec extends UnitSpec with MockitoSugar with ScalaFutures with WithFakeApplication {

  trait Setup {
    val whiteListServiceMock = mock[WhiteListService]
    val continueUrlValidator = new ContinueUrlValidator(whiteListServiceMock)
    implicit val hc = HeaderCarrier()
  }

  "isRelativeOrAbsoluteWhiteListed" should {
    "return true if continueUrl is relative" in new Setup {
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(ContinueUrl("/relative"))) shouldBe true
      verifyZeroInteractions(whiteListServiceMock)
    }

    "return true if continueUrl is absolute whitelisted" in new Setup {
      val url = ContinueUrl("http://absolute/whitelisted")
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(url)).thenReturn(Future(true))
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(url)) shouldBe true
    }

    "return false if continueUrl is not relative or absolute whitelisted" in new Setup {
      val url = ContinueUrl("http://not-relative-or-absolute-whitelisted")
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(url)).thenReturn(Future(false))
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(url)) shouldBe false
    }

  }

  "isRelativeOrInternal" should {
    "return true if continueUrl is relative" in new Setup {
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(ContinueUrl("/relative"))) shouldBe true
      verifyZeroInteractions(whiteListServiceMock)
    }

    "return true if continueUrl is internal" in new Setup {
      val url = ContinueUrl("http://absolute/whitelisted")
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(url)).thenReturn(Future(true))
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(url)) shouldBe true
    }

    "return false if continueUrl is not relative or internal" in new Setup {
      val url = ContinueUrl("http://not-relative-or-absolute-whitelisted")
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(url)).thenReturn(Future(false))
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(url)) shouldBe false
    }

  }
}
