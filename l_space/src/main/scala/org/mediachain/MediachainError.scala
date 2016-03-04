package org.mediachain

import gremlin.scala.Vertex

sealed abstract class GraphError

object GraphError {
  case class MultipleRawBlobsError(vertex: Vertex) extends GraphError
  case class AuthorNotFoundError(vertex: Vertex) extends GraphError

  case class CanonicalNotFound() extends GraphError
  case class RawMetadataNotFound() extends GraphError
  case class PersonNotFound() extends GraphError
  case class VertexNotFound() extends GraphError
}


sealed abstract class MediachainError

object MediachainError {
  case class Graph(error: GraphError) extends MediachainError
}