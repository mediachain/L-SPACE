package io.mediachain.api.util

import cats.data.Xor
import gremlin.scala._
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory
import org.json4s.DefaultFormats
import spray.httpx.Json4sJacksonSupport
import spray.routing._
import spray.http._


object SprayHelpers {

  object JsonSupport extends Json4sJacksonSupport {
    implicit def json4sJacksonFormats = DefaultFormats
  }
  import JsonSupport._

  def withGraph[T](f: Graph => T)(implicit graphFactory: OrientGraphFactory): T = {
    val graph = graphFactory.getTx()
    val result = f(graph)
    graph.close()
    result
  }


  def completeXor[E, T <: AnyRef](f: =>Xor[E, T])
  : Route = ctx => {
      val resultXor = f
      resultXor match {
        case Xor.Left(err) =>
          ctx.complete((500, s"Error completing request: $err"))

        case Xor.Right(result) =>
          ctx.complete(result)
      }
  }
}
