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

package base

import play.api.Configuration
import play.api.mvc.SessionCookieBaker
import play.api.test.Injecting
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, PlainText}
import support.WireMockSpec

trait BaseISpec extends WireMockSpec with Injecting {

  override protected def extraConfig: Map[String, Any] = Map(
    "Test.microservice.services.service-locator.enabled" -> false
  )

  val config = app.injector.instanceOf[Configuration]

  private val cookieBaker = inject[SessionCookieBaker]

  def decryptCookie(mdtpSessionCookieValue: String): Map[String, String] = {
    val applicationCrypto = new ApplicationCrypto(config.underlying)

    applicationCrypto.SessionCookieCrypto.decrypt(Crypted(mdtpSessionCookieValue)) match {
      case PlainText(v) => cookieBaker.decode(v)
      case _            => Map.empty
    }
  }
}
