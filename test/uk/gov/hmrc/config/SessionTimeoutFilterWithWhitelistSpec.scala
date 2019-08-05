package uk.gov.hmrc.config

import org.joda.time.Duration
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import play.api.mvc._
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import org.scalatest.mockito.MockitoSugar
import scala.concurrent.Future

class SessionTimeoutFilterWithWhitelistSpec extends UnitSpec with ScalaFutures with WithFakeApplication with MockitoSugar {

  val requestHeader = FakeRequest(method = "GET", path = "/path/12")

  "apply" should {

    "call next operation function when path in whitelist" in {
      val filter = new SessionTimeoutFilterWithWhitelist(
        timeoutDuration   = Duration.standardSeconds(1),
        whitelistedPaths  = Set("/path/.*"),
        onlyWipeAuthToken = false
      )

      val nextOperationFunction = mock[RequestHeader => Future[Result]]
      when(nextOperationFunction.apply(any())).thenReturn(Future.successful(Results.Ok.withSession(("authToken", "Bearer Token"))))

      whenReady(filter.apply(nextOperationFunction)(requestHeader)){ result =>
        result.session(requestHeader).data("authToken") shouldBe "Bearer Token"
      }

      verify(nextOperationFunction).apply(requestHeader)
    }

    "delegate to SessionTimeoutFilter which clears authToken header from expired session" in {
      val filter = new SessionTimeoutFilterWithWhitelist(
        timeoutDuration   = Duration.standardSeconds(1),
        whitelistedPaths  = Set.empty,
        onlyWipeAuthToken = false
      )

      val nextOperationFunction = mock[RequestHeader => Future[Result]]
      when(nextOperationFunction.apply(any())).thenReturn(Future.successful(Results.Ok.withSession("authToken" -> "Bearer Token")))

      whenReady(filter(nextOperationFunction)(requestHeader.withSession("ts" -> "0"))) { result =>
        result.session(requestHeader).data should not contain "authToken"
      }

      verify(nextOperationFunction).apply(any())
    }

  }

}
