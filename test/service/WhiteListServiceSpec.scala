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

import connectors.SsoConnector
import models.{DomainsResponse, WhiteListedDomains}
import org.scalatest.concurrent.ScalaFutures
import services.{CachedDomainsService, WhiteListService}
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, SafeRedirectUrl}

import scala.concurrent.{ExecutionContext, Future}

class WhiteListServiceSpec extends UnitSpec with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val validDomains = DomainsResponse(WhiteListedDomains(
      externalDomains = Set("www.validexternal.com", "another-valid-domain.com"),
      internalDomains = Set("www.validinternal.gov.uk")), 60
    )

    val mockCachedDomainsService = mock[CachedDomainsService]
    val mockSsoConnector = mock[SsoConnector]
    val whiteListService = new WhiteListService(mockCachedDomainsService, mockSsoConnector)(ExecutionContext.global)
  }

  "white list service" should {

    "return true for continueUrl with a valid internal hostname" in new Setup {
      when(mockCachedDomainsService.getDomains).thenReturn(Future.successful(Some(validDomains)))
      await(whiteListService.getWhitelistedAbsoluteUrl(RedirectUrl("https://www.validinternal.gov.uk"))) shouldBe Some(SafeRedirectUrl("https://www.validinternal.gov.uk"))
    }

    "return true for continueUrl with a valid external hostname" in new Setup {
      when(mockCachedDomainsService.getDomains).thenReturn(Future.successful(Some(validDomains)))
      await(whiteListService.getWhitelistedAbsoluteUrl(RedirectUrl("https://www.validexternal.com"))) shouldBe Some(SafeRedirectUrl("https://www.validexternal.com"))
    }

    "return false for continueUrl with hostname which is not a valid internal or external domain" in new Setup {
      when(mockCachedDomainsService.getDomains).thenReturn(Future.successful(Some(validDomains)))
      await(whiteListService.getWhitelistedAbsoluteUrl(RedirectUrl("https://www.phishing.com"))) shouldBe None
    }

    "return false is the list of valid domains is unavailable" in new Setup {
      when(mockCachedDomainsService.getDomains).thenReturn(Future.successful(None))
      await(whiteListService.getWhitelistedAbsoluteUrl(RedirectUrl("https://www.validexternal.com"))) shouldBe None
    }

    "return false for relative urls" in new Setup {
      when(mockCachedDomainsService.getDomains()(any))
        .thenReturn(Future.successful(Some(DomainsResponse(WhiteListedDomains(Set.empty, Set.empty), 60))))
      await(whiteListService.getWhitelistedAbsoluteUrl(RedirectUrl("/relative-urls-cant-be-whitelisted-by-host"))) shouldBe None
    }

  }

}
