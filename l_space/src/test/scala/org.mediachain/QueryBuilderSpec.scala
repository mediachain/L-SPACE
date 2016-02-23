package org.mediachain

import org.apache.tinkerpop.gremlin.orientdb.OrientGraph
import org.mediachain.Types._
import org.mediachain.Query.buildQuery
import org.specs2.Specification
import gremlin.scala._

object QueryBuilderSpec extends Specification with Orientable {

  def is =
    s2"""
        Uses buildQuery to find a canonical by canonicalID: $findsCanonical
      """


  def findsCanonical = { graph: OrientGraph =>
    val canonical = Canonical.create()
    val v = graph + canonical

    val query = buildQuery(graph.V, ("canonicalID", canonical.canonicalID))
    val queriedCanonical = query.headOption.map(_.toCC[Canonical])

    queriedCanonical must beSome[Canonical].which(c => {
      c.canonicalID == canonical.canonicalID
    })
  }
}
