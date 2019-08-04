package com.base.app

import scala.util.{Failure, Success}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.base.app.routes.Routes

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object WebServer extends App {


  implicit val system = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandleAsync(Routes.handleRequests, "localhost", 9000)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"Server online at http://localhost:9000/\nPress CTRL + C to stop...")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  StdIn.readLine()

}