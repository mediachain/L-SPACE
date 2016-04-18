package io.mediachain.api

import cats.data.Xor
import io.mediachain.BaseSpec
import io.mediachain.api.util.SprayHelpers.{JsonSupport, completeXor}
import org.json4s.JObject
import spray.routing.HttpService
import spray.testkit.Specs2RouteTest
import spray.http._

object SprayHelperSpec extends BaseSpec
  with Specs2RouteTest with HttpService
{
  import JsonSupport._

  def is =
    s2"""
         - completeXor fails the request on Xor.Left $completeXorFailsOnLeft
         - completeXor succeeds on Xor.Right $completeXorSucceedsOnRight
         - completeXor works with case classes via json conversion $completeXorCaseClass
      """


  def actorRefFactory = system

  case class Foo(fooValue: String)

  def testRoute =
    path("xorFail") {
      completeXor {
        Xor.left[String, String]("Error, suckka")
      }
    } ~
      path("xorSuccess") {
        completeXor(Xor.right[String, String]("success!"))
      } ~
      path("xorCaseClass") {
        completeXor(Xor.right[String, Foo](Foo("bar")))
      }

  def completeXorFailsOnLeft =
    Get("/xorFail") ~> sealRoute(testRoute) ~> check {
      status.isFailure must beTrue
      status.intValue must_== 500
    }

  def completeXorSucceedsOnRight =
    Get("/xorSuccess") ~> testRoute ~> check {
      status.isSuccess must beTrue
      responseAs[String] must_== "success!"
    }


  def completeXorCaseClass =
    Get("/xorCaseClass") ~> testRoute ~> check {
      status.isSuccess must beTrue
      responseAs[JObject].values must contain("fooValue" -> "bar")
    }
}
