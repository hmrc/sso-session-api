/*
 * Copyright 2022 HM Revenue & Customs
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

import config.AppConfig
import models.{DomainsResponse, PermittedDomains}
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HeaderNames
import play.api.libs.json.Json
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class SsoDomainsConnectorSpec extends UnitSpec with ScalaFutures {

  trait Setup {
    val mockHttp = mock[HttpClient]
    val serviceBaseURL = "http://mockbaseurl:1234"
    val mockAppConfig = mock[AppConfig]

    val ssoDomainsConnector = new SsoDomainsConnector(mockHttp, mockAppConfig)(ExecutionContext.global)
  }

  "SsoDomainsConnector" should {

    "return Future[DomainsResponse] and max-age value when getDomains() is called" in new Setup {
      when(mockAppConfig.ssoUrl).thenReturn("http://mockbaseurl:1234")
      val mockPermittedDomains = PermittedDomains(Set("domain1.com", "domain2.com"), Set("domain3.com", "domain4.com"))

      when(mockHttp.GET[Either[UpstreamErrorResponse, HttpResponse]](any, any, any)(any, any, any)).thenReturn(Future.successful(
        Right(HttpResponse(
          200,
          json = Json.format[PermittedDomains].writes(mockPermittedDomains),
          Map(HeaderNames.CACHE_CONTROL -> Seq("max-age=33")),
        ))
      ))

      val domains = await(ssoDomainsConnector.getDomains()(HeaderCarrier()))

      domains match {
        case DomainsResponse(PermittedDomains(extDomains, intDomains), maxAge) => {
          extDomains.headOption shouldBe Some("domain1.com")
          intDomains.headOption shouldBe Some("domain3.com")
          maxAge shouldBe 33
        }
        case _ => fail("PermittedDomains expected")
      }
    }

    "return Future[DomainsResponse] and default max-age value when getDomains() is called where no max-age header in http response" in new Setup {
      when(mockAppConfig.ssoUrl).thenReturn("http://mockbaseurl:1234")
      val mockPermittedDomains = PermittedDomains(Set("domain1.com", "domain2.com"), Set("domain3.com", "domain4.com"))

      when(mockHttp.GET[Either[UpstreamErrorResponse, HttpResponse]](any, any, any)(any, any, any)).thenReturn(Future.successful(
        Right(HttpResponse(
          200,
          json = Json.format[PermittedDomains].writes(mockPermittedDomains),
          Map.empty,
        ))
      ))

      val domains = await(ssoDomainsConnector.getDomains()(HeaderCarrier()))

      domains match {
        case DomainsResponse(PermittedDomains(extDomains, intDomains), maxAge) => {
          extDomains.headOption shouldBe Some("domain1.com")
          intDomains.headOption shouldBe Some("domain3.com")
          maxAge shouldBe 60
        }
        case _ => fail("PermittedDomains expected")
      }
    }

  }

}

