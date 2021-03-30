/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc._
import services.{ContinueUrlValidator, PermittedContinueUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SandboxController @Inject() (
    val continueUrlValidator: ContinueUrlValidator,
    controllerComponents:     MessagesControllerComponents
)(implicit val ec: ExecutionContext)
  extends FrontendController(controllerComponents)
  with PermittedContinueUrl {

  def create(continueUrl: RedirectUrl): Action[AnyContent] = Action.async { implicit request =>
    withPermittedContinueUrl(continueUrl) { _ =>
      Future.successful(
        Ok(Json.obj(
          "_links" -> Json.obj(
            "session" -> s"http://schema.org"
          )
        ))
      )
    }
  }

}
