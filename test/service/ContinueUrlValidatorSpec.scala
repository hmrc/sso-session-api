/*
 * Copyright 2024 HM Revenue & Customs
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
import services.{AllowlistService, ContinueUrlValidator}
import support.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, SafeRedirectUrl}

import scala.concurrent.Future

class ContinueUrlValidatorSpec extends UnitSpec with ScalaFutures {

  trait Setup {
    val mockAllowlistService: AllowlistService = mock[AllowlistService]
    val continueUrlValidator = new ContinueUrlValidator(mockAllowlistService)
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "getRelativeOrAbsoluteWhiteListed" should {
    "return a SafeRedirectUrl if continueUrl is relative" in new Setup {
      await(continueUrlValidator.getRelativeOrAbsolutePermitted(RedirectUrl("/relative"))) shouldBe Some(SafeRedirectUrl("/relative"))

      verifyZeroInteractions(mockAllowlistService)
    }

    "return a SafeRedirectUrl if continueUrl is absolute whitelisted" in new Setup {
      val url = RedirectUrl("http://absolute/whitelisted")
      when(mockAllowlistService.getPermittedAbsoluteUrl(eqTo(url))(*))
        .thenReturn(Future.successful(Some(SafeRedirectUrl("http://absolute/whitelisted"))))

      await(continueUrlValidator.getRelativeOrAbsolutePermitted(url)) shouldBe Some(SafeRedirectUrl("http://absolute/whitelisted"))
    }

    "return None if continueUrl is not relative or absolute whitelisted" in new Setup {
      val url = RedirectUrl("http://not-relative-or-absolute-whitelisted")
      when(mockAllowlistService.getPermittedAbsoluteUrl(eqTo(url))(*))
        .thenReturn(Future.successful(None))

      await(continueUrlValidator.getRelativeOrAbsolutePermitted(url)) shouldBe None
    }
  }
}
