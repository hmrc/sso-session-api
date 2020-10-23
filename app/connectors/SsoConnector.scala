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
import play.api.http.HeaderNames
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SsoConnector @Inject() (
    http:      HttpClient,
    appConfig: AppConfig
)(implicit val ec: ExecutionContext)
  extends ApiJsonFormats {

  private lazy val serviceUrl = new URL(appConfig.ssoUrl)

  def createToken(tokenRequest: ApiToken)(implicit hc: HeaderCarrier): Future[URL] = {
    val createTokensUrl = serviceUrl + "/sso/api-tokens"

    for {
      response <- http.POST[ApiToken, Either[UpstreamErrorResponse, HttpResponse]](createTokensUrl, tokenRequest).map {
        case Right(response) => response
        case Left(err)       => throw err
      }
      uri = response.header(HeaderNames.LOCATION).getOrElse(
        throw new RuntimeException("Couldn't get a url from location header ")
      )
    } yield new URL(serviceUrl, uri)
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
