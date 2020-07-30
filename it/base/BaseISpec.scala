package base

import play.api.Configuration
import play.api.mvc.Session
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, PlainText}
import uk.gov.hmrc.gg.test.WireMockSpec

trait BaseISpec extends WireMockSpec {

  override protected def extraConfig: Map[String, Any] = Map(
    "Test.microservice.services.service-locator.enabled" -> false
  )

  val config = app.injector.instanceOf[Configuration]

  def decryptCookie(mdtpSessionCookieValue: String): Map[String, String] = {
    val applicationCrypto = new ApplicationCrypto(config.underlying)

    applicationCrypto.SessionCookieCrypto.decrypt(Crypted(mdtpSessionCookieValue)) match {
      case PlainText(v) => Session.decode(v)
      case _ => Map.empty
    }
  }
}
