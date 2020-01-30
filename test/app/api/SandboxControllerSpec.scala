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

package app.api

import api.SandboxController
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import domains.{ContinueUrlValidator, WhiteListService}
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.binders.ContinueUrl

import scala.concurrent.Future

class SandboxControllerSpec extends UnitSpec with ScalaFutures {
  val whiteListService: WhiteListService = mock[WhiteListService]

  object succeedingValidator extends ContinueUrlValidator(whiteListService) {
    override def isRelativeOrAbsoluteWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = Future.successful(true)
  }

  object failingValidator extends ContinueUrlValidator(whiteListService) {
    override def isRelativeOrAbsoluteWhiteListed(continueUrl: ContinueUrl)(implicit hc: HeaderCarrier): Future[Boolean] = Future.successful(false)
  }

  "SandboxController" should {
    "Return a 200 and the correct JSON for a GET" in {
      val continueUrl = mock[ContinueUrl]
      val controller = new SandboxController(succeedingValidator)
      val result = controller.create(continueUrl)(FakeRequest())

      status(result) shouldBe OK
      contentAsJson(result) \ "_links" \ "session" shouldBe JsDefined(JsString(s"http://schema.org"))
    }

    "Redirect if the ContinueURL is not valid" in {
      val continueUrl = mock[ContinueUrl]
      val controller = new SandboxController(failingValidator)
      val result = controller.create(continueUrl)(FakeRequest())

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "Invalid Continue URL"
    }
  }

}
