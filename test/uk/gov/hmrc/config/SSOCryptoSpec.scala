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

package uk.gov.hmrc.config

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import uk.gov.hmrc.crypto.{PlainContent, PlainText, PlainBytes}

class SSOCryptoSpec extends UnitSpec with ScalaFutures with WithFakeApplication with MockitoSugar {

  lazy val crypto = fakeApplication.injector.instanceOf[SsoCrypto]

  "SSOCrypto" should {
    val plain = PlainText("some text")

    "encrypt and decrypt a plain text object" in {
      val encrypted = crypto.encrypt(plain)
      crypto.decrypt(encrypted) shouldBe plain
    }

    "an encrypted object is different from the plain text" in {
      val encrypted = crypto.encrypt(plain)
      plain.value == encrypted.value shouldBe false
    }

    "encrypt and decrypt a plain bytes object" in {
      val plainBytes = PlainBytes(plain.value.getBytes)
      val encrypted = crypto.encrypt(plainBytes)
      crypto.decryptAsBytes(encrypted).value shouldBe plainBytes.value
    }
  }
}
