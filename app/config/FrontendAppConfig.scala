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

package config

import javax.inject.Inject
import play.api.Configuration
import uk.gov.hmrc.crypto.{Crypted, CryptoGCMWithKeysFromConfig, Decrypter, Encrypter, PlainBytes, PlainContent, PlainText}
import uk.gov.hmrc.gg.config.GenericAppConfig
import uk.gov.hmrc.play.config.{RunMode, ServicesConfig}

class FrontendAppConfig @Inject() (configuration: Configuration) extends Decrypter with Encrypter with ServicesConfig with RunMode with GenericAppConfig {

  val analyticsHost: String = configuration.getString(s"$env.google-analytics.host").getOrElse("service.gov.uk")

  lazy val appName: String = loadConfig("appName")
  lazy val appUrl: String = loadConfig("appUrl")
  lazy val authServiceUrl: String = baseUrl("auth")

  lazy val ssoFeHost: String = configuration.getString(s"$env.sso-fe.host").getOrElse("")

  private def loadConfig(key: String): String = configuration.getString(key).getOrElse(throw new Exception(s"Missing configuration key: $key"))

  val crypto = new CryptoGCMWithKeysFromConfig(baseConfigKey = "sso.encryption", configuration.underlying)
  override def decrypt(reversiblyEncrypted: Crypted): PlainText = crypto.decrypt(reversiblyEncrypted)
  override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = crypto.decryptAsBytes(reversiblyEncrypted)
  override def encrypt(plain: PlainContent): Crypted = crypto.encrypt(plain)
}

