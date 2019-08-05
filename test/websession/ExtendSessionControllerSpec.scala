package websession

import java.awt.image.{DataBufferByte, Raster}
import java.io.ByteArrayInputStream
import java.lang.System.currentTimeMillis
import java.util.Base64
import javax.imageio.ImageIO

import akka.util.ByteString
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import websession.extend.{ExtendSessionController, SessionTimestamp}

class ExtendSessionControllerSpec extends UnitSpec with ScalaFutures with WithFakeApplication with MockitoSugar {
  val controller = ExtendSessionController
  val base64TransparentImage = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAABBJREFUeNpi+P//PwNAgAEACPwC/tuiTRYAAAAASUVORK5CYII="

  def base64(bytes: ByteString): String = {
    val ba = bytes.toList.toArray
    Base64.getEncoder.encodeToString(ba)
  }

  "Extend session Controller" must {
    "return a status of 200 and a transparent image when called with a request with no session, but do not set a timestamp" in {
      implicit val fakeRequestWithEmptySession = FakeRequest("", "")
      val result = controller.extend("CDFE39B4-625E-4375-8D5E-AF3440CA867F")(fakeRequestWithEmptySession)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("image/png")
      base64(contentAsBytes(result)) shouldBe base64TransparentImage
      result.session.get(SessionKeys.lastRequestTimestamp) shouldBe None
    }

    "return a status of 200 and a transparent image and set a timestamp when called with a request with a session that does not contain a timestamp" in {
      implicit val fakeRequestWithArbitrarySession = FakeRequest().withSession("AKey" -> "AValue")
      val result = controller.extend("CDFE39B4-625E-4375-8D5E-AF3440CA867F")(fakeRequestWithArbitrarySession)

      status(result) shouldBe OK
      contentType(result) shouldBe Some("image/png")
      base64(contentAsBytes(result)) shouldBe base64TransparentImage
      result.session.get(SessionKeys.lastRequestTimestamp).isDefined shouldBe true
    }

    "return a status of 200 and a transparent image and leave a timestamp when called with a request with a session that has an unexpired timestamp in contain a timestamp" in {
      val unexpiredTime = currentTimeMillis() - 100
      implicit val fakeRequestWithArbitrarySession = FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> unexpiredTime.toString)

      val result = controller.extend("CDFE39B4-625E-4375-8D5E-AF3440CA867F")(fakeRequestWithArbitrarySession)
      status(result) shouldBe OK
      contentType(result) shouldBe Some("image/png")
      base64(contentAsBytes(result)) shouldBe base64TransparentImage

      val previousSessionTime = fakeRequestWithArbitrarySession.session.get(SessionKeys.lastRequestTimestamp).get
      val extendedSessionTime = result.session.get(SessionKeys.lastRequestTimestamp).get

      previousSessionTime < extendedSessionTime shouldBe true

    }

    "return a status of 200 and a transparent image and leave a timestamp when called with a request with a session that has an expired timestamp in contain a timestamp but doesn't extend the session" in {

      val expiredTime = currentTimeMillis() - (16 * 60 * 1000)
      implicit val fakeRequestWithArbitrarySession = FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> expiredTime.toString)

      val result = controller.extend("CDFE39B4-625E-4375-8D5E-AF3440CA867F")(fakeRequestWithArbitrarySession)
      status(result) shouldBe OK
      contentType(result) shouldBe Some("image/png")
      base64(contentAsBytes(result)) shouldBe base64TransparentImage

      val previousSessionTime = fakeRequestWithArbitrarySession.session.get(SessionKeys.lastRequestTimestamp).get
      val extendedSessionTime = result.session.get(SessionKeys.lastRequestTimestamp).get

      previousSessionTime < extendedSessionTime shouldBe false
    }
  }
}
