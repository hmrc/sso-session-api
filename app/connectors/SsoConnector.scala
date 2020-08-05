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

import com.google.inject.Inject
import config.AppConfig
import javax.inject.Singleton
import models.ApiToken
import play.api.cache.AsyncCacheApi
import play.api.http.HeaderNames
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SsoConnector @Inject() (
    val cache: AsyncCacheApi,
    val http:  HttpClient,
    appConfig: AppConfig
)(implicit val ec: ExecutionContext)
  extends ApiJsonFormats
  with PlayCache {

  private lazy val serviceUrl = new URL(appConfig.ssoUrl)

  def getRootAffordance(reader: JsValue => String)(implicit hc: HeaderCarrier): Future[URL] = {
    for {
      resp <- withCache(serviceUrl)
      affordanceUri = reader(resp.json)
    } yield new URL(serviceUrl, affordanceUri)
  }

  def createToken(tokenRequest: ApiToken)(implicit hc: HeaderCarrier): Future[URL] = {
    for {
      createTokensUrl <- getRootAffordance(json => (json \ "api-tokens").as[String])
      response <- http.POST[ApiToken, Either[UpstreamErrorResponse, HttpResponse]](createTokensUrl.toString, tokenRequest).map {
        case Right(response) => response
        case Left(err)       => throw err
      }
      uri = response.header(HeaderNames.LOCATION).getOrElse(
        throw new RuntimeException("Couldn't get a url from location header ")
      )
    } yield new URL(createTokensUrl, uri)
  }

  def getTokenDetails(tokenUrl: URL)(implicit hc: HeaderCarrier): Future[ApiToken] = {
    http.GET[ApiToken](tokenUrl.toString)
  }
}

case class SsoInSessionInfo(bearerToken: String, sessionId: String)

trait ApiJsonFormats {
  implicit val tokenRequestRead: Format[ApiToken] = (
    (JsPath \ "bearer-token").format[String] and
    (JsPath \ "session-id").format[String] and
    (JsPath \ "continue-url").format[String] and
    (JsPath \ "user-id").formatNullable[String]
  ) (ApiToken.apply, unlift(ApiToken.unapply))

  implicit val ssoInSessionInfoRead: Reads[SsoInSessionInfo] = Json.reads[SsoInSessionInfo]

}

// See http://www.ehcache.org/generated/2.10.1/pdf/Ehcache_Configuration_Guide.pdf. Define max size and MemoryStoreEvictionPolicy to control eviction policy.
trait PlayCache {

  def http: HttpGet

  def cache: AsyncCacheApi

  implicit val ec: ExecutionContext

  private val cacheControlPattern = "max-age=(\\d+)".r

  def withCache(url: URL)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val urlAsString = url.toString

    cache.get[HttpResponse](urlAsString).flatMap {
      case Some(cachedValue) =>
        Future.successful(cachedValue)
      case None =>
        http.GET[Either[UpstreamErrorResponse, HttpResponse]](urlAsString).flatMap {
          case Right(response) =>
            response.header(HeaderNames.CACHE_CONTROL).collect {
              case cacheControlPattern(seconds) =>
                cache.set(urlAsString, response, seconds.toLong.seconds).map(_ => response)
            }.getOrElse(Future.successful(response))

          case Left(err) =>
            throw err
        }
    }
  }
}

