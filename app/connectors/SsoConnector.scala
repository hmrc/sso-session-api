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

package connectors

import java.net.URL

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.Configuration
import play.api.Mode.Mode
import play.api.cache.CacheApi
import play.api.http.HeaderNames
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.config.{FrontendGlobal, WSHttp}
import uk.gov.hmrc.gg.config.GenericAppConfig
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.LoggingDetails
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext
import websession._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SsoConnector @Inject() (override val cache: CacheApi) extends ApiJsonFormats with PlayCache with ServicesConfig with GenericAppConfig {

  implicit def getExecutionContext(implicit loggingDetails: LoggingDetails): ExecutionContext = MdcLoggingExecutionContext.fromLoggingDetails(loggingDetails)
  def serviceUrl = new URL(baseUrl("sso"))
  def http: WSHttp = WSHttp

  def getRootAffordance(reader: JsValue => String)(implicit hc: HeaderCarrier): Future[URL] = {
    for {
      resp <- withCache(serviceUrl)
      affordanceUri = reader(resp.json)
    } yield new URL(serviceUrl, affordanceUri)
  }

  def createToken(tokenRequest: ApiToken)(implicit hc: HeaderCarrier): Future[URL] = {
    for {
      createTokensUrl <- getRootAffordance(json => (json \ "api-tokens").as[String])
      response <- http.POST[ApiToken, HttpResponse](createTokensUrl.toString, tokenRequest)
      uri = response.header(HeaderNames.LOCATION).getOrElse(
        throw new RuntimeException("Couldn't get a url from location header ")
      )
    } yield new URL(createTokensUrl, uri)
  }

  def getTokenDetails(tokenUrl: URL)(implicit hc: HeaderCarrier): Future[ApiToken] = {
    http.GET[ApiToken](tokenUrl.toString)
  }
}

case class SsoInSessionInfo(bearerToken: String, sessionId: String, userId: String)

trait ApiJsonFormats {
  implicit val tokenRequestRead: Format[ApiToken] = (
    (JsPath \ "bearer-token").format[String] and
    (JsPath \ "session-id").format[String] and
    (JsPath \ "continue-url").format[String] and
    (JsPath \ "user-id").format[String]
  ) (ApiToken.apply, { case ApiToken(b, s, c, u) => (b, s, c, u) })

  implicit val ssoInSessionInfoRead: Reads[SsoInSessionInfo] = Json.reads[SsoInSessionInfo]

}

// See http://www.ehcache.org/generated/2.10.1/pdf/Ehcache_Configuration_Guide.pdf. Define max size and MemoryStoreEvictionPolicy to control eviction policy.
trait PlayCache {

  def http: HttpGet

  def cache: CacheApi

  implicit def getExecutionContext(implicit loggingDetails: LoggingDetails): ExecutionContext

  private val cacheControlPattern = "max-age=(\\d+)".r

  def withCache(url: URL)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val urlAsString = url.toString
    val resp = cache.get[HttpResponse](urlAsString).fold {
      val eventualResponse = http.GET[HttpResponse](urlAsString)
      eventualResponse.onSuccess {
        case response =>
          response.header(HeaderNames.CACHE_CONTROL).collect {
            case cacheControlPattern(seconds) => cache.set(urlAsString, response, seconds.toLong.seconds)
          }
      }
      eventualResponse
    }(Future.successful)
    resp
  }
}

