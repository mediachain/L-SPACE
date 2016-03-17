package io.mediachain.translation

import java.io.File

import scala.io.Source
import cats.data.Xor
import io.mediachain.Types.{RawMetadataBlob, PhotoBlob}
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory
import org.json4s._
import com.fasterxml.jackson.core.JsonFactory
import io.mediachain.translation.JsonLoader.parseJArray
import org.json4s.jackson.Serialization.write

trait Translator {
  val name: String
  val version: Int
  def translate(source: JObject): Xor[TranslationError, PhotoBlob]
}

trait FSLoader[T <: Translator] {
  val translator: T

  val pairI: Iterator[Xor[TranslationError, (JObject, String)]]
  val path: String

  def loadPhotoBlobs: Iterator[Xor[TranslationError,(PhotoBlob, RawMetadataBlob)]] = {
    pairI.map { pairXor => pairXor.flatMap { case (json, raw) =>
      translator.translate(json).map { (_, RawMetadataBlob(None, raw))}
    }}
  }
}

trait DirectoryWalkerLoader[T <: Translator] extends FSLoader[T] {
  private val fileI: Iterator[File] = DirectoryWalker.findWithExtension(new File(path), ".json").iterator
  private val rawI = fileI.map(Source.fromFile(_).mkString)
  private val jsonI = fileI.map { file =>
      val jf = new JsonFactory
      val parser = jf.createParser(file)
      JsonLoader.parseJOBject(parser)
        .leftMap(err =>
          TranslationError.ParsingFailed(new RuntimeException(err)))
    }
  val pairI = jsonI.zip(rawI).map { case (jsonXor, raw) =>
      jsonXor.map((_,raw))
  }
}

trait FlatFileLoader[T <: Translator] extends FSLoader[T] {
  val pairI = {
    val jf = new JsonFactory
    val parser = jf.createParser(new File(path))

    parser.nextToken

    implicit val formats = org.json4s.DefaultFormats
    parseJArray(parser).map {
      case Xor.Right(json: JObject) => Xor.right((json, write(json)))
      case err @ (Xor.Left(_) | Xor.Right(_)) => Xor.left(TranslationError.ParsingFailed(new RuntimeException(err.toString)))
    }
  }
}

object TranslatorDispatcher {
  def dispatch(partner: String, path: String) = {
    //println("partner: " + partner + ", path: " + path)
    val translator = partner match {
      //case "moma" => new moma.MomaLoader(path)
      case "tate" => new tate.TateLoader(path)
    }

    val blobS = translator.loadPhotoBlobs

    val url = sys.env.get("ORIENTDB_URL").getOrElse(throw new Exception("ORIENTDB_URL required"))
    val user = sys.env.get("ORIENTDB_USER").getOrElse(throw new Exception("ORIENTDB_USER required"))
    val password = sys.env.get("ORIENTDB_PASSWORD").getOrElse(throw new Exception("ORIENTDB_PASSWORD required"))
    val graph = new OrientGraphFactory(url, user, password).getNoTx()
  }
}
