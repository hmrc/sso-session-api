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

package connectors

import java.net.URL

import domains.{DomainsResponse, WhiteListedDomains}
import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.gg.config.GenericAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SsoDomainsConnector @Inject() (http: HttpClient) extends ServicesConfig with GenericAppConfig {

  private val DefaultTTL = 60
  private val MaxAgeHeaderPattern = "max-age=(\\d+)".r

  implicit def getExecutionContext(implicit loggingDetails: LoggingDetails): ExecutionContext = MdcLoggingExecutionContext.fromLoggingDetails(loggingDetails)
  def serviceUrl = new URL(baseUrl("sso"))

  implicit val whiteListedDomainsFormat: OFormat[WhiteListedDomains] = Json.format[WhiteListedDomains]

  def getDomains()(implicit hc: HeaderCarrier): Future[DomainsResponse] = {
    http.GET(s"$serviceUrl/sso/domains").map { response =>
      DomainsResponse(Json.parse(response.body).as[WhiteListedDomains], getMaxAgeFrom(response.header(HeaderNames.CACHE_CONTROL)).getOrElse(DefaultTTL))
    }
  }

  private def getMaxAgeFrom(cacheControlHeader: Option[String]): Option[Int] = {
    cacheControlHeader flatMap {
      case MaxAgeHeaderPattern(t) => Some(t.toInt)
      case _                      => None
    }
  }
}
