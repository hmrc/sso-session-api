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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{get, okJson, stubFor}
import models.{DomainsResponse, PermittedDomains}
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json.Json
import play.api.test.Injecting
import play.mvc.Http.HeaderNames
import support.WireMockSpec
import uk.gov.hmrc.http.HeaderCarrier

class SsoDomainsConnectorISpec extends WireMockSpec with ScalaFutures with Injecting {

  trait Setup {
    val ssoDomainsConnector: SsoDomainsConnector = inject[SsoDomainsConnector]
  }

  "SsoDomainsConnector" should {

    "return Future[DomainsResponse] and max-age value when getDomains() is called" in new Setup {
      val mockPermittedDomains: PermittedDomains = PermittedDomains(Set("domain1.com", "domain2.com"), Set("domain3.com", "domain4.com"))

      stubFor(
        get("/sso/domains")
          .willReturn(
            okJson(Json.toJson(mockPermittedDomains).toString).withHeader(HeaderNames.CACHE_CONTROL, "max-age=33")
          )
      )

      val domains: DomainsResponse = await(ssoDomainsConnector.getDomains()(HeaderCarrier()))

      domains match {
        case DomainsResponse(PermittedDomains(extDomains, intDomains), maxAge) =>
          extDomains.headOption shouldBe Some("domain1.com")
          intDomains.headOption shouldBe Some("domain3.com")
          maxAge                shouldBe 33
      }
    }

    "return Future[DomainsResponse] and default max-age value when getDomains() is called where no max-age header in http response" in new Setup {
      val mockPermittedDomains: PermittedDomains = PermittedDomains(Set("domain1.com", "domain2.com"), Set("domain3.com", "domain4.com"))

      stubFor(
        get("/sso/domains")
          .willReturn(
            okJson(Json.toJson(mockPermittedDomains).toString)
          )
      )

      val domains: DomainsResponse = await(ssoDomainsConnector.getDomains()(HeaderCarrier()))

      domains match {
        case DomainsResponse(PermittedDomains(extDomains, intDomains), maxAge) =>
          extDomains.headOption shouldBe Some("domain1.com")
          intDomains.headOption shouldBe Some("domain3.com")
          maxAge                shouldBe 60
      }
    }

  }

}
