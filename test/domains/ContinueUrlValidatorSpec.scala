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

package domains

import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ContinueUrlValidatorSpec extends UnitSpec with ScalaFutures {

  trait Setup {
    val whiteListServiceMock: WhiteListService = mock[WhiteListService]
    val continueUrlValidator = new ContinueUrlValidator(whiteListServiceMock)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "isRelativeOrAbsoluteWhiteListed" should {
    "return true if continueUrl is relative" in new Setup {
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(ContinueUrl("/relative"))) shouldBe true
      verifyZeroInteractions(whiteListServiceMock)
    }

    "return true if continueUrl is absolute whitelisted" in new Setup {
      val url = ContinueUrl("http://absolute/whitelisted")
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(eqTo(url), *)(*)).thenReturn(Future(true))
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(url)) shouldBe true
    }

    "return false if continueUrl is not relative or absolute whitelisted" in new Setup {
      val url = ContinueUrl("http://not-relative-or-absolute-whitelisted")
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(eqTo(url), *)(*)).thenReturn(Future(false))
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
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(eqTo(url), *)(*)).thenReturn(Future(true))
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(url)) shouldBe true
    }

    "return false if continueUrl is not relative or internal" in new Setup {
      val url = ContinueUrl("http://not-relative-or-absolute-whitelisted")
      when(whiteListServiceMock.isAbsoluteUrlWhiteListed(eqTo(url), *)(*)).thenReturn(Future(false))
      await(continueUrlValidator.isRelativeOrAbsoluteWhiteListed(url)) shouldBe false
    }

  }
}
