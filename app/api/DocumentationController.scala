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

package api

import javax.inject.Inject
import controllers.Assets
import play.api.Configuration
import play.api.http.MimeTypes
import play.api.mvc.{Action, AnyContent, Controller}

class DocumentationController @Inject() (assets: Assets, config: Configuration) extends Controller {

  private lazy val whitelist = config.getStringSeq("api.access.version-1.0.whitelistedApplicationIds").getOrElse(Nil)

  def definition(): Action[AnyContent] = Action { _ =>
    Ok(views.txt.definition(whitelist)).withHeaders(CONTENT_TYPE -> MimeTypes.JSON)
  }

  def raml(version: String, file: String) = {
    assets.at(s"/public/api/conf/$version", file)
  }
}
