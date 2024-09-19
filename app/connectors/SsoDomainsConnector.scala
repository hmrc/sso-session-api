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

import config.AppConfig
import models.{DomainsResponse, PermittedDomains}
import play.api.http.HeaderNames
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SsoDomainsConnector @Inject() (http: HttpClientV2, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private val DefaultTTL = 60
  private val MaxAgeHeaderPattern = "max-age=(\\d+)".r

  private lazy val serviceUrl = new URL(appConfig.ssoUrl)

  def getDomains()(implicit hc: HeaderCarrier): Future[DomainsResponse] = {
    http.get(url"$serviceUrl/sso/domains").execute[Either[UpstreamErrorResponse, HttpResponse]].map {
      case Left(err) => throw err
      case Right(response) =>
        DomainsResponse(
          Json.parse(response.body).as[PermittedDomains],
          getMaxAgeFrom(response.header(HeaderNames.CACHE_CONTROL)).getOrElse(DefaultTTL)
        )
    }
  }

  private def getMaxAgeFrom(cacheControlHeader: Option[String]): Option[Int] = {
    cacheControlHeader flatMap {
      case MaxAgeHeaderPattern(t) => Some(t.toInt)
      case _                      => None
    }
  }
}
