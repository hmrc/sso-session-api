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

import org.scalatest.concurrent.ScalaFutures
import services.{ContinueUrlValidator, WhiteListService}
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, SafeRedirectUrl}

import scala.concurrent.Future

class ContinueUrlValidatorSpec extends UnitSpec with ScalaFutures {

  trait Setup {
    val mockWhitelistService: WhiteListService = mock[WhiteListService]
    val continueUrlValidator = new ContinueUrlValidator(mockWhitelistService)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "getRelativeOrAbsoluteWhiteListed" should {
    "return a SafeRedirectUrl if continueUrl is relative" in new Setup {
      await(continueUrlValidator.getRelativeOrAbsoluteWhiteListed(RedirectUrl("/relative"))) shouldBe Some(SafeRedirectUrl("/relative"))

      verifyZeroInteractions(mockWhitelistService)
    }

    "return a SafeRedirectUrl if continueUrl is absolute whitelisted" in new Setup {
      val url = RedirectUrl("http://absolute/whitelisted")
      when(mockWhitelistService.getWhitelistedAbsoluteUrl(eqTo(url))(*))
        .thenReturn(Future.successful(Some(SafeRedirectUrl("http://absolute/whitelisted"))))

      await(continueUrlValidator.getRelativeOrAbsoluteWhiteListed(url)) shouldBe Some(SafeRedirectUrl("http://absolute/whitelisted"))
    }

    "return None if continueUrl is not relative or absolute whitelisted" in new Setup {
      val url = RedirectUrl("http://not-relative-or-absolute-whitelisted")
      when(mockWhitelistService.getWhitelistedAbsoluteUrl(eqTo(url))(*))
        .thenReturn(Future.successful(None))

      await(continueUrlValidator.getRelativeOrAbsoluteWhiteListed(url)) shouldBe None
    }
  }
}
