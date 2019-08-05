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

package service

import connectors.SsoConnector
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.domains.{CachedDomainsService, DomainsResponse, WhiteListService, WhiteListedDomains}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class WhiteListServiceSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()

    val validDomains = DomainsResponse(WhiteListedDomains(
      externalDomains = Set("www.validexternal.com", "another-valid-domain.com"),
      internalDomains = Set("www.validinternal.gov.uk")), 60
    )

    val cachedDomainsService = mock[CachedDomainsService]
    val ssoConnectorMock = mock[SsoConnector]

    when(cachedDomainsService.getDomains).thenReturn(Future.successful(Some(validDomains)))

    val whiteListService = new WhiteListService(cachedDomainsService, ssoConnectorMock)
  }

  "white list service" should {

    "return true for continueUrl with a valid internal hostname" in new Setup {
      whiteListService.isAbsoluteUrlWhiteListed(ContinueUrl("https://www.validinternal.gov.uk")).futureValue shouldBe true
    }

    "return true for continueUrl with a valid external hostname" in new Setup {
      whiteListService.isAbsoluteUrlWhiteListed(ContinueUrl("https://www.validexternal.com")).futureValue shouldBe true
    }

    "return false for continueUrl with hostname which is not a valid internal or external domain" in new Setup {
      whiteListService.isAbsoluteUrlWhiteListed(ContinueUrl("https://www.phishing.com")).futureValue shouldBe false
    }

    "return false is the list of valid domains is unavailable" in new Setup {
      when(cachedDomainsService.getDomains).thenReturn(Future.successful(None))
      whiteListService.isAbsoluteUrlWhiteListed(ContinueUrl("https://www.validexternal.com")).futureValue shouldBe false
    }

    "return false for badly formatted continueUrls" in new Setup {
      // Probably hard todo because of the constructor validation in ContinueUrl but we can't completely trust a url returned as a string
      val badlyFormattedContinueUrlMock = mock[ContinueUrl]
      when(badlyFormattedContinueUrlMock.url).thenReturn("not a url")
      whiteListService.isAbsoluteUrlWhiteListed(badlyFormattedContinueUrlMock).futureValue shouldBe false
    }

    "return false for relative urls" in new Setup {
      whiteListService.isAbsoluteUrlWhiteListed(ContinueUrl("/relative-urls-cant-be-whitelisted-by-host")).futureValue shouldBe false
    }

  }

}
