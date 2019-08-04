package com.base.app.routes

import akka.http.scaladsl.model.HttpMethods.{DELETE, POST, PUT}
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import com.base.app.models.caseclass._
import com.base.app.models.dao.DAOS
import org.json4s.native.JsonMethods.{parse, pretty, render}
import org.json4s.{DefaultFormats, Extraction}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object Routes extends DAOS {

  implicit val jsonFormats = DefaultFormats
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def emptyResponse = HttpResponse.apply(404, Nil,
    HttpEntity(ContentTypes.`application/json`,
      pretty(render(Extraction.decompose(ResponseType($type = "type_error", None))))
    ))

  def notAuthorized = HttpResponse.apply(401, Nil,
    HttpEntity(ContentTypes.`application/json`,
      pretty(render(Extraction.decompose(ResponseType($type = "not_authorized", None))))
    ))

  def handleRequests: HttpRequest => Future[HttpResponse] = {


    case HttpRequest(DELETE, Uri.Path("/ws_api/remove"), headers: Seq[HttpHeader], entity, _) => {
      var optionUser: Option[User] = None
      for {
        Authorization(BasicHttpCredentials(username, password)) <- headers
      } yield {
        optionUser = Await.result(find(User(0, username, password, "")).map(r => r), 10 seconds)
      }

      if ( optionUser.isDefined && optionUser.get.userType == UserType.admin.toString ) {
        var optionMessage: Option[TableMessages] = None

        Await.result(entity.dataBytes.runForeach(r => {
          optionMessage = Some(parse(r.utf8String).extract[TableMessages])
          println("I'm coming from await  ", optionMessage)
        }).map(r => r), 10 seconds)

        if (optionMessage.isDefined && optionMessage.get.$type == "remove_table") {

          for {
            message <- removeTable(optionMessage.get).map(m => m)
          } yield {
              HttpResponse.apply(200, Nil, HttpEntity(ContentTypes.`application/json`,
                pretty(render(Extraction.decompose(message)))
              ))
          }

        } else
          Future(emptyResponse)

      } else Future(notAuthorized)

    }

    case HttpRequest(PUT, Uri.Path("/ws_api/update"), headers: Seq[HttpHeader], entity, _) => {
      var optionUser: Option[User] = None
      for {
        Authorization(BasicHttpCredentials(username, password)) <- headers
      } yield {
        optionUser = Await.result(find(User(0, username, password, "")).map(r => r), 10 seconds)
      }

      if ( optionUser.isDefined && optionUser.get.userType == UserType.admin.toString ) {
        var optionTableCU: Option[TableCU] = None

        Await.result(entity.dataBytes.runForeach(r => {
          optionTableCU = Some(parse(r.utf8String).extract[TableCU])
          println("I'm coming from await  ", optionTableCU)
        }).map(r => r), 10 seconds)

        if (optionTableCU.isDefined && optionTableCU.get.$type == "update_table"
                                    && optionTableCU.get.table.id.isDefined) {

          for {
            tupleTableCU <- updateTable(optionTableCU.get).map(m => m)
          } yield {
            if (tupleTableCU._2) {
              HttpResponse.apply(200, Nil, HttpEntity(ContentTypes.`application/json`,
                pretty(render(Extraction.decompose(
                  TableMessages($type = "update_failed", id = optionTableCU.get.table.id.get)))
              )))
            } else
            HttpResponse.apply(200, Nil, HttpEntity(ContentTypes.`application/json`,
              pretty(render(Extraction.decompose(tupleTableCU._1)))
            ))
          }

        } else
          Future(emptyResponse)

      } else Future(notAuthorized)

    }

    case HttpRequest(POST, Uri.Path("/ws_api/add"), headers: Seq[HttpHeader], entity, _) => {
      var optionUser: Option[User] = None
      for {
        Authorization(BasicHttpCredentials(username, password)) <- headers
      } yield {
        optionUser = Await.result(find(User(0, username, password, "")).map(r => r), 10 seconds)
      }

      if ( optionUser.isDefined && optionUser.get.userType == UserType.admin.toString ) {
        var optionTableCU: Option[TableCU] = None

        Await.result(entity.dataBytes.runForeach(r => {
          optionTableCU = Some(parse(r.utf8String).extract[TableCU])
          println("I'm coming from await  ", optionTableCU)
        }).map(r => r), 10 seconds)

        if (optionTableCU.isDefined && optionTableCU.get.$type == "add_table") {

          for {
            tableCU <- addTable(optionTableCU.get).map(m => m)
          } yield {
              HttpResponse.apply(200, Nil, HttpEntity(ContentTypes.`application/json`,
                pretty(render(Extraction.decompose(tableCU)))
              ))
          }

        } else
          Future(emptyResponse)

      } else Future(notAuthorized)

    }

    case HttpRequest(POST, Uri.Path("/ws_api/login"), _, entity, _) => {

      val emptyResponse = HttpResponse.apply(401, Nil,
        HttpEntity(ContentTypes.`application/json`,
          pretty(render(Extraction.decompose(ResponseType($type = "login_failed", None))))
        ))
      var optionLoginData: Option[LoginData] = None

      Await.result(entity.dataBytes.runForeach(r => {
        optionLoginData = Some(parse(r.utf8String).extract[LoginData])
        println("I'm coming from await  ", optionLoginData)
      }).map(r => r), 10 seconds)
      if (optionLoginData.isDefined && optionLoginData.get.$type == "login") {
        for {
          userOption <- find(User(0, optionLoginData.get.username, optionLoginData.get.password, "")).map(r => r)
        } yield {
          if (userOption.isDefined) {
            HttpResponse.apply(200, Nil, HttpEntity(ContentTypes.`application/json`,
              pretty(render(Extraction.decompose(ResponseType($type = "login_successfull", userType = Some(userOption.get.userType)) )))
            ))
          } else emptyResponse
        }

      } else
        Future(emptyResponse)
    }

    case HttpRequest(POST, Uri.Path("/ws_api/ping"), _, entity, _) => {

      var optionPingPong: Option[PingPong] = None

      Await.result(entity.dataBytes.runForeach(r => {
        optionPingPong = Some(parse(r.utf8String).extract[PingPong])
        println("I'm coming from await  ", optionPingPong)
      }).map(m => m), 10 seconds)

      if (optionPingPong.isDefined && optionPingPong.get.$type == "ping") {
        Future(HttpResponse.apply(200, Nil, HttpEntity(ContentTypes.`application/json`,
          pretty(render(Extraction.decompose(PingPong($type = "pong", optionPingPong.get.seq))))
        )))
      } else
        Future(emptyResponse)
    }

    case HttpRequest(POST, Uri.Path("/ws_api/subscribe"), _, entity, _) => {

      var optionResponseType: Option[ResponseType] = None

      Await.result(entity.dataBytes.runForeach(r => {
        optionResponseType = Some(parse(r.utf8String).extract[ResponseType])
        println("I'm coming from await  ", optionResponseType)
      }).map(m => m), 10 seconds)
      if (optionResponseType.isDefined && optionResponseType.get.$type == "subscribe_tables") {

        for (result <- listTable)
        yield HttpResponse(200, Nil,
            HttpEntity(ContentTypes.`application/json`,
              pretty(render(Extraction.decompose(result)))))

      } else
        Future(emptyResponse)

    }

    case HttpRequest(POST, Uri.Path("/ws_api/unsubscribe"), _, entity, _) => {

      var optionResponseType: Option[ResponseType] = None

      Await.result(entity.dataBytes.runForeach(r => {
        optionResponseType = Some(parse(r.utf8String).extract[ResponseType])
        println("I'm coming from await  ", optionResponseType)
      }).map(m => m), 10 seconds)
      if (optionResponseType.isDefined && optionResponseType.get.$type == "unsubscribe_tables") {

        Future(HttpResponse(200, Nil, HttpEntity.Empty))

      } else
        Future(emptyResponse)

    }

    case _ => Future(HttpResponse.apply(StatusCodes.BadRequest,Nil, HttpEntity.Empty))
  }

}
