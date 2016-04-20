package io.mediachain.api

import io.mediachain.{BaseSpec, GraphFixture}
import io.mediachain.Types._
import spray.testkit.Specs2RouteTest
import spray.http.StatusCodes._
import gremlin.scala._
import io.mediachain.util.orient.MigrationHelper
import org.json4s._
import org.json4s.JsonDSL._

object LSpaceServiceSpec extends BaseSpec
  with Specs2RouteTest with LSpaceService {
  def actorRefFactory = system

  val graphFactory = MigrationHelper.newInMemoryGraphFactory()
  val context = GraphFixture.Context(graphFactory.getTx)

  import JsonSupport._

  def is =
    s2"""
       returns a canonical from GET "/" $returnsFirstCanonical
       returns a canonical by ID $returnsACanonical
       returns a canonical's rev history by ID $returnsASubtree
       returns the works by an author $returnsWorks
      """


  def returnsFirstCanonical = {
    Get("/canonicals") ~> baseRoute ~> check {
      status === OK
      val canonicalId = context.objects.imageBlobCanonical.canonicalID

      responseAs[JArray] \\ "canonicalID" \\ classOf[JString] must contain(
        canonicalId
      )
    }
  }

  def returnsACanonical = {
    val canonicalId = context.objects.imageBlobCanonical.canonicalID
    Get("/canonicals/" + canonicalId) ~> baseRoute ~> check {
      status === OK
      responseAs[JObject] \ "canonicalID" must_== JString(canonicalId)
    }
    Get("/canonicals/" + canonicalId + "?with_raw=1") ~> baseRoute ~> check {
      status === OK
      val r = responseAs[JObject]
      r \ "canonicalID" must_== JString(canonicalId)
      r \ "raw" \ "blob" must beLike {
        case JString(rawString) => rawString.trim must startWith("{")
      }
    }
  }


  private def matchBlob(blob: ImageBlob) =
    beLike[JValue] {
      case blobJObject: JObject => {
        blobJObject.obj must contain(
          "title" -> JString(blob.title),
          "description" -> JString(blob.description),
          "date" -> JString(blob.date)
        )
      }
    }

  private def matchRevision(blob: ImageBlob) = {
    beLike[JValue] {
      case obj: JObject => (obj \ "artefact") must matchBlob(blob)
    }
  }

  def returnsASubtree = {
    val canonicalId = context.objects.imageBlobCanonical.canonicalID
    Get("/canonicals/" + canonicalId + "/history") ~> baseRoute ~> check {
      status === OK
      val r = responseAs[JObject]
      (r \ "canonicalID") must be_== (JString(canonicalId))

      val revisions = (r \ "revisions").asInstanceOf[JArray]
      revisions.arr must contain(
        matchRevision(context.objects.imageBlob),
        matchRevision(context.objects.modifiedImageBlob)
      )
    }
  }

  def returnsWorks = {
    val personCanonicalID = context.objects.personCanonical.canonicalID
    val imageBlobCanonicalID = context.objects.imageBlobCanonical.canonicalID
    val imageByDuplicateAuthorCanonicalID =
      context.objects.imageByDuplicatePersonCanonical.canonicalID

    Get(s"/canonicals/$personCanonicalID/works") ~> baseRoute ~> check {
      val r = responseAs[JObject]
      (r \ "canonicalID") aka "person canonical ID" must_== JString(personCanonicalID)
      val worksList = r \ "works" \\ "canonicalID" \\ classOf[JString]

      worksList must contain(
        imageBlobCanonicalID,
        imageByDuplicateAuthorCanonicalID
      )
    }
  }
}
