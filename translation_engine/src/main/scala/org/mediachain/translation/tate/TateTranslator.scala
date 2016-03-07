package org.mediachain.translation.tate

import org.mediachain.translation.{JsonLoader, Translator, TranslationContext}


object TateTranslator extends Translator {
  val name = "TateCreativeCommons"
  val version = 1

  import cats.data.Xor
  import org.mediachain.translation.TranslationError, TranslationError.InvalidFormat
  import org.mediachain.Types._

  import org.json4s._
  implicit val formats = org.json4s.DefaultFormats

  case class TateContext(id: String, translator: Translator = TateTranslator)
    extends TranslationContext[PhotoBlob] {
    def translate(source: String): Xor[TranslationError, (PhotoBlob, RawMetadataBlob)] = {
      JsonLoader.loadObjectFromString(source)
        .flatMap(loadArtwork)
        .map(photoBlob => {
          (photoBlob, RawMetadataBlob(None, source))
        })
    }
  }

  private case class Contributor(fc: String, role: String)
  private case class Artwork(title: String,
                             medium: Option[String],
                             dateText: Option[String],
                             contributors: List[Contributor])


  def loadArtwork(obj: JObject): Xor[TranslationError, PhotoBlob] = {
    val artwork = obj.extractOpt[Artwork]
    val result = artwork.map { a =>

      val artists = for {
        c <- a.contributors
        if c.role == "artist"
      } yield Person(None, c.fc)

      PhotoBlob(None,
        a.title,
        a.medium.getOrElse(""),
        a.dateText.getOrElse(""),
        artists.headOption)
    }

    Xor.fromOption(result, InvalidFormat())
  }

}