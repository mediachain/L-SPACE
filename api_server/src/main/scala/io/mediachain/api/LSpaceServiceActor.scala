package io.mediachain.api

import java.util.UUID

import akka.actor.{Actor, Props}
import cats.data.Xor
import spray.routing.HttpService
import io.mediachain.api.util.SprayHelpers
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory
import io.mediachain.api.util.SprayHelpers._
import spray.http._

object LSpaceServiceActor {
  def props(graphFactory: OrientGraphFactory): Props =
    Props(classOf[LSpaceServiceActor], graphFactory)
}

class LSpaceServiceActor(val graphFactory: OrientGraphFactory)
  extends Actor with LSpaceService {

  def actorRefFactory = context

  def receive = runRoute(baseRoute)
}



trait LSpaceService extends HttpService {
  import SprayHelpers.JsonSupport._

  implicit val graphFactory: OrientGraphFactory


  import operations.CanonicalQueries._
  val canonicalRoutes =
    (get & pathPrefix("canonicals")) {
      // GET "/canonicals"
      pathEnd {
        parameter("page" ? 0) { page =>
          complete {
            withGraph(listCanonicals(page))
          }
        }
      } ~
        pathPrefix(JavaUUID) { canonicalID: UUID =>
          // GET "/canonicals/some-canonical-id"
          pathEnd {
            parameter("with_raw" ? 0 ) { with_raw =>
              complete {
                withGraph(canonicalWithID(canonicalID, with_raw == 1))
              }
            }
          } ~
            // GET "/canonicals/some-canonical-id/history
            path("history") {
              complete {
                withGraph(historyForCanonical(canonicalID))
              }
            } ~
            // GET "/canonicals/persons-canonical-id/works"
            path("works") {
              complete {
                withGraph(worksForPersonWithCanonicalID(canonicalID))
              }
            }
        }
    }


  val ingestionRoutes =
    (post & pathPrefix("ingest")) {
      complete(???)
    }


  import operations.Merging._
  val mergeRoutes =
    (post & path("merge" / JavaUUID / "into" / JavaUUID)) {
      (childCanonicalID, parentCanonicalID) =>
        completeXor {
          withGraph(mergeCanonicals(childCanonicalID, parentCanonicalID))
        }
    }

  val baseRoute =
    canonicalRoutes ~
      mergeRoutes ~
      ingestionRoutes
}
