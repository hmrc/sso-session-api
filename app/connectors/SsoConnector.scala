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

import java.net.URL
import com.google.inject.Inject
import config.AppConfig

import javax.inject.Singleton
import models.ApiToken
import play.api.http.HeaderNames
import play.api.libs.functional.syntax.*
import play.api.libs.json.Reads.*
import play.api.libs.json.*
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SsoConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig
)(implicit val ec: ExecutionContext)
    extends ApiJsonFormats {

  private lazy val serviceUrl = new URL(appConfig.ssoUrl)

  def createToken(tokenRequest: ApiToken)(implicit hc: HeaderCarrier): Future[URL] = {
    val createTokensUrl = url"$serviceUrl/sso/api-tokens"

    for {
      response <- http.post(createTokensUrl).withBody(Json.toJson(tokenRequest)).execute[Either[UpstreamErrorResponse, HttpResponse]].map {
                    case Right(response) => response
                    case Left(err)       => throw err
                  }
      uri = response
              .header(HeaderNames.LOCATION)
              .getOrElse(
                throw new RuntimeException("Couldn't get a url from location header ")
              )
    } yield new URL(serviceUrl, uri)
  }

  def getTokenDetails(tokenUrl: URL)(implicit hc: HeaderCarrier): Future[ApiToken] = {
    http.get(tokenUrl).execute[ApiToken]
  }
}

case class SsoInSessionInfo(bearerToken: String, sessionId: String)

trait ApiJsonFormats {
  implicit val tokenRequestRead: Format[ApiToken] = (
    (JsPath \ "bearer-token").format[String] and
      (JsPath \ "session-id").format[String] and
      (JsPath \ "continue-url").format[String] and
      (JsPath \ "user-id").formatNullable[String]
  )(ApiToken.apply, o => Tuple.fromProductTyped(o))

  implicit val ssoInSessionInfoRead: Reads[SsoInSessionInfo] = Json.reads[SsoInSessionInfo]

}
