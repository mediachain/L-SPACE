package io.mediachain.api

import java.util.UUID

import akka.actor.{Actor, Props}
import cats.data.Xor
import spray.routing.HttpService
import spray.http._
import io.mediachain.Types._
import org.json4s.{DefaultFormats, JObject}
import spray.httpx.Json4sJacksonSupport
import gremlin.scala._
import io.mediachain.util.orient.GraphConnectionPool
import io.mediachain.Traversals
import org.json4s.JsonAST.JString

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.blocking

object LSpaceServiceActor {
  def props(graphPool: GraphConnectionPool): Props =
    Props(classOf[LSpaceServiceActor], graphPool)
}

class LSpaceServiceActor(val graphPool: GraphConnectionPool)
  (implicit val executionContext: ExecutionContext)
  extends Actor with LSpaceService {

  def actorRefFactory = context

  def receive = runRoute(baseRoute)
}

object JsonSupport extends Json4sJacksonSupport {
  implicit def json4sJacksonFormats = DefaultFormats
}

trait LSpaceService extends HttpService {
  import JsonSupport._

  implicit val executionContext: ExecutionContext
  val graphPool: GraphConnectionPool

  /**
    * Helper to obtain a graph connection and perform some operation `f`
    * that returns a result of type `T`.  Returns a `Future[T]` that will
    * fail if `f` throws an exception. Otherwise the result of `f` will be
    * returned as the Future's success value.
    *
    * The `Graph` passed in to `f` will be closed immediately after `f` is
    * invoked and returned to the connection pool, so make sure you don't
    * capture a reference to it or otherwise depend on its continued validity!
    *
    */
  def withGraph[T](f: Graph => T)
  : Future[T] = {
    // obtain a new graph instance from the pool
    graphPool.getGraph
      .map { graph =>
        val result = try {
          // signal to the ExecutionContext that the operation may block
          blocking {
            // perform the operation
            f(graph)
          }
        } finally {
          // release the graph back to the pool
          graph.close()
        }
        // return result
        result
      }
  }


  val PAGE_SIZE = 20
  def listCanonicals(page: Int)
  : Future[List[Canonical]] =
    withGraph { graph =>
      val first = page * PAGE_SIZE
      val last = first + PAGE_SIZE

      graph.V.hasLabel[Canonical].toCC[Canonical]
        .range(first, last).toList
    }

  def canonicalWithID(canonicalID: UUID)
  : Future[Option[Canonical]] =
    withGraph { graph =>
      Traversals.canonicalsWithUUID(graph.V, canonicalID)
        .toCC[Canonical]
        .headOption
    }

  def historyForCanonical(canonicalID: UUID): Future[JObject] = ???


  val baseRoute =
    pathPrefix("canonicals") {
      get {
        // GET "/canonicals"
        pathEnd {
          parameter("page" ? 0) { page: Int =>
            onSuccess(listCanonicals(page)) { canonicals: List[Canonical] =>
              complete(canonicals)
            }
          }
        } ~
          pathPrefix(JavaUUID) { canonicalID: UUID =>
            // GET "/canonicals/some-canonical-id"
            pathEnd {
              onSuccess(canonicalWithID(canonicalID))
                complete(_)
            } ~
              // GET "/canonicals/some-canonical-id/history
              path("history") {
                onSuccess(historyForCanonical(canonicalID))
                  complete(_)
              }
          }
      }
    }
}
