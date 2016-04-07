package io.mediachain

import cats.data.Xor
import Types._
import core.GraphError
import core.GraphError._
import gremlin.scala._
import io.mediachain.util.GremlinUtils._
import Traversals.{GremlinScalaImplicits, VertexImplicits}
import com.orientechnologies.orient.core.exception.OStorageException

object Ingress {

  case class BlobAddResult(blobVertex: Vertex, canonicalVertex: Vertex)

  def ingestBlobBundle(graph: Graph, bundle: BlobBundle
    , rawMetadata: Option[RawMetadataBlob] = None)
  : Xor[GraphError, Canonical] = withTransactionXor(graph) {
    val addResultXor: Xor[GraphError, BlobAddResult] =
      bundle.content match {
        case image: ImageBlob => {
          addImageBlob(graph, image, rawMetadata)
        }

        case person: Person => {
          addPerson(graph, person, rawMetadata)
        }

        case _ => Xor.left(InvalidBlobBundle())
      }

    addResultXor.flatMap { addResult =>
      val (blobV, canonicalV) = (addResult.blobVertex, addResult.canonicalVertex)

      val relationshipXors = bundle.relationships.map {
        case BlobBundle.Author(author) =>
          addPerson(graph, author, rawMetadata).flatMap { result =>
            defineAuthorship(blobV, result.canonicalVertex)
          }
      }

      val canonical = canonicalV.toCC[Canonical]
      relationshipXors.foldLeft(Xor.right[GraphError, Canonical](canonical)) {
        case (Xor.Left(err), _) => Xor.left(err)
        case (_, Xor.Left(err)) => Xor.left(err)
        case (_, Xor.Right(_)) => Xor.right(canonical)
      }
    }
  }


  def attachRawMetadata(blobV: Vertex, raw: RawMetadataBlob): Unit =
  withTransaction(blobV.graph) {
    val graph = blobV.graph

    // add the raw metadata to the graph if it doesn't already exist
    val rawV = Traversals.rawMetadataBlobsWithExactMatch(graph.V, raw)
      .headOption
      .getOrElse(graph + raw)

    // check if there's already an edge from the blob vertex to the raw metadata vertex
    val existingBlobs: Set[Vertex] = try {
      rawV.in(TranslatedFrom).toSet
    } catch {
      case _: OStorageException => Set()
      case t: Throwable => throw t
    }

    if (!existingBlobs.contains(blobV)) {
      blobV --- TranslatedFrom --> rawV
    }
  }

  def defineAuthorship(blobV: Vertex, authorCanonicalVertex: Vertex):
  Xor[GraphError, Unit] = withTransactionXor(blobV.graph) {
    val authorshipAlreadyDefined: Xor[GraphError, Boolean] =
    try {
      blobV.lift.findAuthorXor.map { authorCanonical: Canonical =>
        authorCanonical.canonicalID ==
          authorCanonicalVertex.toCC[Canonical].canonicalID
      }
    } catch {
      case _: OStorageException => Xor.right(false)
      case t: Throwable => throw t
    }

    authorshipAlreadyDefined.map { defined =>
      if (!defined) {
        blobV --- AuthoredBy --> authorCanonicalVertex
      }
    }
  }


  def addMetadataBlob[T <: MetadataBlob with Product : Marshallable]
  (graph: Graph, blob: T, rawMetadataOpt: Option[RawMetadataBlob] = None):
  Xor[GraphError, BlobAddResult] = withTransactionXor(graph) {
    val existingVertex: Option[Vertex] = blob match {
      case image: ImageBlob =>
        Traversals.imageBlobsWithExactMatch(graph.V, image).headOption
      case person: Person =>
        Traversals.personBlobsWithExactMatch(graph.V, person).headOption
      case _ => None
    }

    val resultXor: Xor[GraphError, (Vertex, Vertex)] =
      existingVertex.map { v =>
        Xor.fromOption(
          Traversals.getCanonical(v.lift).headOption,
          CanonicalNotFound())
          .map(canonicalV => (v, canonicalV))
      }.getOrElse {
        val v = graph + blob
        val canonicalV = graph + Canonical.create()
        canonicalV --- DescribedBy --> v
        Xor.right((v, canonicalV))
      }
    
    rawMetadataOpt.foreach { raw =>
      resultXor.foreach { res =>
        attachRawMetadata(res._1, raw)
      }
    }

    resultXor.map { res => BlobAddResult(res._1, res._2) }
  }


  def addPerson(graph: Graph, person: Person, rawMetadata: Option[RawMetadataBlob] = None):
  Xor[GraphError, BlobAddResult] =
    addMetadataBlob(graph, person, rawMetadata)


  def addImageBlob(graph: Graph, image: ImageBlob, rawMetadata: Option[RawMetadataBlob] = None):
  Xor[GraphError, BlobAddResult] =
    addMetadataBlob(graph, image, rawMetadata)


  def modifyImageBlob(graph: Graph, parentVertex: Vertex, photo: ImageBlob, raw: Option[RawMetadataBlob] = None):
  Xor[GraphError, Canonical] = {
    Traversals.imageBlobsWithExactMatch(graph.V, photo)
      .findCanonicalXor
      .map(Xor.right)
      .getOrElse {
        val childVertex = graph + photo
        parentVertex --- ModifiedBy --> childVertex

        raw.foreach(attachRawMetadata(childVertex, _))

        childVertex.lift.findCanonicalXor
          .map(Xor.right)
          .getOrElse(Xor.left(CanonicalNotFound()))
      }
  }
}

