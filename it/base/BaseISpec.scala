package base

import play.api.Configuration
import play.api.mvc.SessionCookieBaker
import play.api.test.Injecting
import uk.gov.hmrc.crypto.{ApplicationCrypto, Crypted, PlainText}
import uk.gov.hmrc.gg.test.WireMockSpec

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
      case _ => Map.empty
    }
  }
}
