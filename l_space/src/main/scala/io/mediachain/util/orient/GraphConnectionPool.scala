package io.mediachain.util.orient

import gremlin.scala.Graph
import org.apache.tinkerpop.gremlin.orientdb.{OrientGraph, OrientGraphFactory}

import scala.concurrent.{ExecutionContext, Future, blocking}

trait GraphConnectionPool {
  def getGraph: Future[Graph]
}

class OrientGraphPool(val factory: OrientGraphFactory)
  (implicit ec: ExecutionContext)
  extends GraphConnectionPool {

  /**
    * Obtains a new graph instance from the `OrientGraphFactory`'s connection
    * pool.  Returns a `Future[Graph]`, since acquiring the connection may
    * block if the pool is at max capacity.
    * @return a `Future` that resolves to an open `Graph` connection.
    */
  def getGraph: Future[Graph] = Future {
    blocking(factory.getTx())
  }
}
