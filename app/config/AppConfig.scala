/*
 * Copyright 2023 HM Revenue & Customs
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

package config

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.crypto.{Crypted, SymmetricCryptoFactory, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) extends Decrypter with Encrypter {

  lazy val ssoFeHost: String = configuration.getOptional[String]("sso-fe.host").getOrElse("")

  lazy val ssoUrl: String = servicesConfig.baseUrl("sso")

  val crypto = SymmetricCryptoFactory.aesGcmCryptoFromConfig(baseConfigKey = "sso.encryption", configuration.underlying)
  override def decrypt(reversiblyEncrypted: Crypted): PlainText = crypto.decrypt(reversiblyEncrypted)
  override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = crypto.decryptAsBytes(reversiblyEncrypted)
  override def encrypt(plain: PlainContent): Crypted = crypto.encrypt(plain)
}

