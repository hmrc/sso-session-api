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

package auth

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FrontendAuthConnector @Inject() (httpClient: HttpClient, frontendAppConfig: FrontendAppConfig) {
  def getAuthUri()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AuthResponse]] = {
    httpClient.GET[Option[AuthResponse]](s"${frontendAppConfig.authServiceUrl}/auth/authority")
  }
}

case class AuthResponse(uri: String)

object AuthResponse {
  implicit val format: OFormat[AuthResponse] = Json.format[AuthResponse]
}

