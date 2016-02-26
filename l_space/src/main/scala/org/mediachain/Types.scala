package org.mediachain

import com.orientechnologies.orient.core.id.ORecordId

object Types {
  import gremlin.scala._
  import java.util.UUID

  type ElementID = ORecordId

  val DescribedBy = "described-by"
  val ModifiedBy  = "modified-by"
  val AuthoredBy  = "authored-by"

  val IDKey          = Key[String]("id")
  val CanonicalIDKey = Key[String]("canonicalID")
  val BlobKey        = Key[String]("blob")
  val NameKey        = Key[String]("name")
  val TitleKey       = Key[String]("title")
  val DescriptionKey = Key[String]("description")
  val DateKey        = Key[String]("date")

  /**
    * Convert from the AnyRef returned by Vertex.id()
    * to an Option[ElementID]
 *
    * @param v a Vertex
    * @return Some(ElementID), or None if no id exists
    */
  def vertexId(v: Vertex): Option[ElementID] = {
    Option(v.id).map(id => id.asInstanceOf[ElementID])
  }

  trait VertexClass {
    def getID(): Option[ElementID]

    def vertex(graph: Graph): Option[Vertex] = {
      val id = getID().getOrElse(throw new IllegalStateException("Malformed vertex object: no id"))
      graph.V(id).headOption()
    }

    protected def withInsertHelper(graph: Graph)(fn: => Vertex):
    Vertex = getID()
      .flatMap { _ => vertex(graph) }
      .getOrElse(fn)

    def insert(graph: Graph): Vertex
  }

  trait VertexObject {
    def label: String
  }

  case class Canonical(id: Option[ElementID],
                       canonicalID: String) extends VertexClass {
    def getID(): Option[ElementID] = id

    def insert(graph: Graph): Vertex = withInsertHelper(graph) {
      graph + (Canonical.label, CanonicalIDKey -> canonicalID)
    }
  }

  object Canonical extends VertexObject {
    def create(): Canonical = {
      Canonical(None, UUID.randomUUID.toString)
    }

    def apply(v: Vertex): Canonical = {
      Canonical(
        vertexId(v),
        v.value[String]("canonicalID")
      )
    }

    def label = "Canonical"
  }

  sealed trait MetadataBlob extends VertexClass

  case class RawMetadataBlob(id: Option[ElementID],
                             blob: String) extends MetadataBlob {
    def getID(): Option[ElementID] = id

    def insert(graph: Graph): Vertex = withInsertHelper(graph) {
      graph + (RawMetadataBlob.label, BlobKey -> blob)
    }
  }

  object RawMetadataBlob extends VertexObject {
    def label = "RawMetadataBlob"
  }

  case class Person(id: Option[ElementID],
                    name: String) extends MetadataBlob {
    def getID(): Option[ElementID] = id

    def insert(graph: Graph): Vertex = withInsertHelper(graph) {
      graph + (Person.label, NameKey -> name)
    }
  }

  object Person extends VertexObject {
    def create(name: String) = {
      Person(None, name)
    }

    def apply(v: Vertex): Option[Person] = {
      if (v.label() == "Person") {
        Some(
          Person(
            vertexId(v),
            name = v.value("name")
          )
        )
      } else {
        None
      }
    }

    def label = "Person"
  }

  case class PhotoBlob(id: Option[ElementID],
                       title: String,
                       description: String,
                       date: String,
                       author: Option[Person]) extends MetadataBlob {
    def getID(): Option[ElementID] = id

    def insert(graph: Graph): Vertex = withInsertHelper(graph) {
      graph + (PhotoBlob.label,
        TitleKey -> title,
        DescriptionKey -> description,
        DateKey -> date)
    }
  }

  object PhotoBlob extends VertexObject {
    def apply(v: Vertex): Option[PhotoBlob] = {
      if (v.label() == "PhotoBlob") {
        Some(
          PhotoBlob(
            vertexId(v),
            title = v.value("title"),
            description = v.value("description"),
            date = v.value("date"),
            author = None // FIXME(yusef)
          )
        )
      } else {
        None
      }
    }

    def label = "PhotoBlob"
  }
}

