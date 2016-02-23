package org.mediachain

object Types {
  import gremlin.scala._
  import java.util.UUID
  import shapeless._

  val DescribedBy = "described-by"
  val AuthoredBy  = "authored-by"

  trait Queryable {
    def queryTerms: QueryTerms[_]
  }

  case class QueryTerms[L <: HList](terms: Tuple2[String, String] :: L)(implicit val c: LUBConstraint[L, Tuple2[String, String]])

  @label("Canonical")
  case class Canonical(@id id: String,
                       canonicalID: String) extends Queryable {
    def queryTerms = {
      QueryTerms(("canonicalID", canonicalID) :: HNil)
    }
  }

  object Canonical {
    def create(): Canonical = {
      Canonical("", UUID.randomUUID.toString)
    }

    def apply(v: Vertex): Canonical = {
      Canonical(
        v.value[String]("id"),
        v.value[String]("canonicalID")
      )
    }
  }

  sealed trait MetadataBlob

  @label("RawMetadataBlob")
  case class RawMetadataBlob(@id id: String,
                             blob: String) extends MetadataBlob

  @label("Person")
  case class Person(@id id: String,
                    name: String) extends MetadataBlob

  object Person {
    def apply(v: Vertex): Option[Person] = {
      if (v.label() == "Person") {
        Some(
          Person(
            id = v.value("id"),
            name = v.value("name")
          )
        )
      } else {
        None
      }
    }
  }

  @label("PhotoBlob")
  case class PhotoBlob(@id id: String,
                       title: String,
                       description: String,
                       date: String,
                       author: Option[Person]) extends MetadataBlob
}

