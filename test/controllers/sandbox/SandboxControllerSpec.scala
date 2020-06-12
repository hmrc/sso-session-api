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

package controllers.sandbox

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json._
import play.api.mvc.MessagesControllerComponents
import play.api.test.{FakeRequest, Injecting}
import services.{ContinueUrlValidator, WhiteListService}
import uk.gov.hmrc.gg.test.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.binders.{RedirectUrl, SafeRedirectUrl}

import scala.concurrent.{ExecutionContext, Future}

class SandboxControllerSpec extends UnitSpec with GuiceOneAppPerSuite with Injecting {
  val whiteListService: WhiteListService = mock[WhiteListService]

  object succeedingValidator extends ContinueUrlValidator(whiteListService) {
    override def getRelativeOrAbsoluteWhiteListed(continueUrl: RedirectUrl)(implicit hc: HeaderCarrier): Future[Option[SafeRedirectUrl]] = {
      Future.successful(Some(SafeRedirectUrl(continueUrl.unsafeValue)))
    }
  }

  object failingValidator extends ContinueUrlValidator(whiteListService) {
    override def getRelativeOrAbsoluteWhiteListed(continueUrl: RedirectUrl)(implicit hc: HeaderCarrier): Future[Option[SafeRedirectUrl]] = {
      Future.successful(None)
    }
  }

  "SandboxController" should {
    "Return a 200 and the correct JSON for a GET" in {
      val continueUrl = RedirectUrl("/a")
      val messagesControllerComponents: MessagesControllerComponents = inject[MessagesControllerComponents]

      val controller = new SandboxController(succeedingValidator, messagesControllerComponents)(ExecutionContext.global)
      val result = controller.create(continueUrl)(FakeRequest())

      status(result) shouldBe OK
      contentAsJson(result) \ "_links" \ "session" shouldBe JsDefined(JsString(s"http://schema.org"))
    }

    "Redirect if the ContinueURL is not valid" in {
      val messagesControllerComponents: MessagesControllerComponents = inject[MessagesControllerComponents]
      val controller = new SandboxController(failingValidator, messagesControllerComponents)(ExecutionContext.global)
      val result = controller.create(RedirectUrl("/a"))(FakeRequest())

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) shouldBe "Invalid Continue URL"
    }
  }

}
