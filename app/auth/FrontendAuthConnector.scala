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

package auth

import com.google.inject.Inject
import javax.inject.Singleton
import uk.gov.hmrc.config.WSHttp
import uk.gov.hmrc.gg.config.GenericAppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.frontend.auth.connectors.AuthConnector
import uk.gov.hmrc.play.frontend.auth.connectors.domain.Authority

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FrontendAuthConnector extends AuthConnector with ServicesConfig with GenericAppConfig {
  val serviceUrl = baseUrl("auth")
  lazy val http = WSHttp

  def getAuthority(authUri: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[Authority]] = {
    http.GET[Option[Authority]](serviceUrl + authUri)
  }
}

