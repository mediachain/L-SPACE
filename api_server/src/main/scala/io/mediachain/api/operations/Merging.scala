package io.mediachain.api.operations

import java.util.UUID

import gremlin.scala._
import io.mediachain.Types._
import io.mediachain.Merge
import io.mediachain.Traversals, Traversals._, Traversals.Implicits._
import cats.data.Xor
import io.mediachain.core.GraphError


object Merging {

  def mergeCanonicals(childCanonicalID: UUID, parentCanonicalID: UUID)
    (graph: Graph): Xor[GraphError, Canonical] =
    for {
      parent <- graph.V ~> canonicalsWithUUID(parentCanonicalID) >>
        findCanonicalXor

      child <- graph.V ~> canonicalsWithUUID(childCanonicalID) >>
        findCanonicalXor

      mergeResult <- Merge.mergeCanonicals(graph, child, parent)
    } yield mergeResult


}
