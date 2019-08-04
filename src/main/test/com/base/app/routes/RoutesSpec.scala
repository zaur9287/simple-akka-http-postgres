package com.base.app.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpEntity, HttpRequest, MediaTypes, StatusCodes, Uri}
import akka.http.scaladsl.model.HttpMethods._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.testkit.ScalatestRouteTest


class RoutesSpec extends WordSpec  with Matchers with  ScalaFutures with ScalatestRouteTest {

  def sendRequest(req: HttpRequest) =
    Source.single(req).via(
      Http(system).outgoingConnection(host = "localhost",  port = 9999)).runWith(Sink.head)

  "login uri " should {
    "can return user type as 'admin'" in {

      val request = sendRequest(HttpRequest(POST, "/ws_api/login", Nil, HttpEntity(MediaTypes.`application/json`, """ { "$type": "login", "username": "zaur", "password": "zaur" } """)))
      whenReady(request) { response =>
        response.status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual """{ "$type": "login_successfull", "userType": "admin" }"""
      }

    }

    "can return user type as 'user'" in {
      // tests:
      val request = sendRequest(HttpRequest(POST, "/ws_api/login", Nil, HttpEntity(MediaTypes.`application/json`, """ { "$type": "login", "username": "fiqi", "password": "fiqi" } """)))
      whenReady(request) { response =>
        response.status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual """{ "$type": "login_successfull", "userType": "admin" }"""
      }
    }

    "can return unauthorized (there is not user in database)" in {
      // tests:
      val request = sendRequest(HttpRequest(POST, "/ws_api/login", Nil, HttpEntity(MediaTypes.`application/json`, """ { "$type": "login", "username": unauth_user", "password": "unknown" } """)))
      whenReady(request) { response =>
        response.status shouldBe StatusCodes.Unauthorized
        responseAs[String] shouldEqual """{ "$type": "login_failed" }"""
      }
    }
  }

  "pinging the server" should {
    val request = sendRequest(HttpRequest(POST, "/ws_api/ping", Nil, HttpEntity(MediaTypes.`application/json`, """ { "$type": "ping", "seq": 15 } """)))
    whenReady(request) { response =>
      response.status shouldBe StatusCodes.OK
      responseAs[String] shouldEqual """{ "$type": "pong", "seq": 15 }"""
    }
  }

  "subscribing to the table" should {
    val request = sendRequest(HttpRequest(POST, "/ws_api/subscribe", Nil, HttpEntity(MediaTypes.`application/json`, """ { "$type": "subscribe_tables" } """)))
    whenReady(request) { response =>
      response.status shouldBe StatusCodes.OK
      responseAs[String] should include("table_list")
    }
  }

  "unsubscribing to the table" should {
    val request = sendRequest(HttpRequest(POST, "/ws_api/unsubscribe", Nil, HttpEntity(MediaTypes.`application/json`, """ { "$type": "unsubscribe_tables" } """)))
    whenReady(request) { response =>
      response.status shouldBe StatusCodes.OK
      responseAs[String] should ""
    }
  }

  "adding table row uri - this api for only admin privileged users" should {

    val request = sendRequest(HttpRequest(POST, "/ws_api/add", Nil, HttpEntity(MediaTypes.`application/json`,
      """ {"$type": "add_table", "after_id": 1,
        |"table": { "name": "table - Foo Fighters", "participants": 4 }} """.stripMargin)))

    whenReady(request) { response =>
      response.status shouldBe StatusCodes.OK
      responseAs[String] should startWith("""{"$type": "table_added"""")
    }

    "user has not enough privilege adding data to the database" in {
      val request = sendRequest(HttpRequest(POST, "/ws_api/add", Nil, HttpEntity(MediaTypes.`application/json`,
        """ {"$type": "add_table", "after_id": 1,
          |"table": { "name": "table - Foo Fighters", "participants": 4 }} """.stripMargin)))

      whenReady(request) { response =>
        response.status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual """{ "$type": "not_authorized" }"""
      }
    }

  }

  "updating table row uri - this api for only admin privileged users" should {

    val request = sendRequest(HttpRequest(POST, "/ws_api/update", Nil, HttpEntity(MediaTypes.`application/json`,
      """ {"$type": "update_table",
        |"table": { "id": 4 "name": "table - Foo Fighters", "participants": 4 }} """.stripMargin)))

    whenReady(request) { response =>
      response.status shouldBe StatusCodes.OK
      responseAs[String] should  startWith(oneOf("""{"$type": "table_updated"""", """{"$type": "update_failed""""))
    }

    "user has not enough privilege to changing data in the database" in {
      val request = sendRequest(HttpRequest(POST, "/ws_api/update", Nil, HttpEntity(MediaTypes.`application/json`,
        """ {"$type": "update_table",
          |"table": { "id": 4 "name": "table - Foo Fighters", "participants": 4 }} """.stripMargin)))

      whenReady(request) { response =>
        response.status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual """{ "$type": "not_authorized" }"""
      }
    }

  }

  "deleting table row uri - this api for only admin privileged users" should {

    val request = sendRequest(HttpRequest(POST, "/ws_api/delete", Nil, HttpEntity(MediaTypes.`application/json`,
      """ {"$type": "update_table",
        |"table": { "id": 4 "name": "table - Foo Fighters", "participants": 4 }} """.stripMargin)))

    whenReady(request) { response =>
      response.status shouldBe StatusCodes.OK
      responseAs[String] should  startWith(oneOf("""{"$type": "table_removed"""", """{"$type": "removal_failed""""))
    }

    "user has not enough privilege to changing data in the database" in {
      val request = sendRequest(HttpRequest(POST, "/ws_api/delete", Nil, HttpEntity(MediaTypes.`application/json`,
        """ {"$type": "update_table",
          |"table": { "id": 4 "name": "table - Foo Fighters", "participants": 4 }} """.stripMargin)))

      whenReady(request) { response =>
        response.status shouldBe StatusCodes.OK
        responseAs[String] shouldEqual """{ "$type": "not_authorized" }"""
      }
    }

  }

}

//  test ("TESTING") {
//    val request = sendRequest(HttpRequest(POST, "/ws_api/login", Nil, HttpEntity(MediaTypes.`application/json`, """ { "$type": "login", "username": "zaur", "password": "zaur" } """)))
//    whenReady(request) { response =>
//      response.status shouldBe StatusCodes.NotFound
//    }
//
//  }
//
//import akka.http.scaladsl.server._
//import Directives._
//import akka.http.javadsl.settings.RoutingSettings
//import akka.http.scaladsl.model.{HttpEntity, MediaTypes, StatusCodes}
//import akka.http.scaladsl.model.StatusCodes.Success
//import akka.http.scaladsl.server.Route
//import akka.http.scaladsl.testkit.ScalatestRouteTest
//import com.base.app.models.caseclass.LoginData
//import org.scalatest.{Matchers, WordSpec}
//
//import scala.util.{Failure, Success}
//
//class RoutesSpec extends WordSpec with Matchers with ScalatestRouteTest {
//
////  case HttpRequest(DELETE, Uri.Path("/ws_api/remove"), headers: Seq[HttpHeader], entity, _) => {
//  val route = Routes.handleRequests
//
//
////  val otherRoute =
////    post {
////        path("/ws_api/login") {
////          entity(as[LoginData]){ json =>
////            complete{
////              case Success(value) => {}
////              case Failure(ex) => {}
////            }
////          }
////        }
////    }
//
//
////  Post("/ws_api/login", HttpEntity(MediaTypes.`application/json`, """{ "$type": "login", "username" : "zaur", "password": "zaur" }""")) ~>
////    route ~> check {
////    responseAs[String] shouldEqual """{ "$type": "login_successful", "user_type": "admin" }"""
////  }
//
//
//    val smallRoute =
//    get {
//      concat(
//        pathSingleSlash {
//          complete {
//            "Captain on the bridge!"
//          }
//        },
//        path("ping") {
//          complete("PONG!")
//        }
//      )
//    }
//
//  "The service" should {
//
//    "return a greeting for GET requests to the root path" in {
//      // tests:
//      Get() ~> smallRoute ~> check {
//        responseAs[String] shouldEqual "Captain on the bridge!"
//      }
//    }
//
//    "return a 'PONG!' response for GET requests to /ping" in {
//      // tests:
//      Get("/ping") ~> smallRoute ~> check {
//        responseAs[String] shouldEqual "PONG!"
//      }
//    }
//
//    "leave GET requests to other paths unhandled" in {
//      // tests:
//      Get("/kermit") ~> smallRoute ~> check {
//        handled shouldBe false
//      }
//    }
//
//    "return a MethodNotAllowed error for PUT requests to the root path" in {
//      // tests:
//      Put() ~> Route.seal(smallRoute) ~> check {
//        status shouldEqual StatusCodes.MethodNotAllowed
//        responseAs[String] shouldEqual "HTTP method not allowed, supported methods: GET"
//      }
//    }
//  }
//}