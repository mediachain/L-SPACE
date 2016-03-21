package io.mediachain.translation

import java.io.File

import scala.io.Source
import cats.data.Xor
import io.mediachain.Types.{Canonical, PhotoBlob, RawMetadataBlob}
import org.apache.tinkerpop.gremlin.orientdb.{OrientGraph, OrientGraphFactory}
import org.json4s._
import com.fasterxml.jackson.core.JsonFactory
import io.mediachain.core.{Error, TranslationError}
import io.mediachain.Ingress
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
    pairI.map { pairXor =>
      pairXor.flatMap { case (json, raw) =>
        translator.translate(json).map {
          (_, RawMetadataBlob(None, raw))
        }
      }
    }
  }
}

trait DirectoryWalkerLoader[T <: Translator] extends FSLoader[T] {
  private val fileI: Iterator[File] = DirectoryWalker.findWithExtension(new File(path), ".json")
  private val rawI = fileI.map(Source.fromFile(_).mkString)
  private val jsonI = {
    val jf = new JsonFactory
    fileI.map { file =>
      val parser = jf.createParser(file)
      parser.nextToken

      JsonLoader.parseJOBject(parser)
        .leftMap(err =>
          TranslationError.ParsingFailed(new RuntimeException(err + " at " + file.toString)))
    }
  }
  val pairI = jsonI.zip(rawI).map {
    case (jsonXor, raw) => jsonXor.map((_,raw))
    case _ => throw new RuntimeException("Should never get here")
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
  // TODO: move + inject me
  def getGraph: OrientGraph = {
    val url = sys.env.getOrElse("ORIENTDB_URL", throw new Exception("ORIENTDB_URL required"))
    val user = sys.env.getOrElse("ORIENTDB_USER", throw new Exception("ORIENTDB_USER required"))
    val password = sys.env.getOrElse("ORIENTDB_PASSWORD", throw new Exception("ORIENTDB_PASSWORD required"))
    val graph = new OrientGraphFactory(url, user, password).getNoTx()

    graph
  }
  def dispatch(partner: String, path: String) = {
    val translator = partner match {
      case "moma" => new moma.MomaLoader(path)
      case "tate" => new tate.TateLoader(path)
    }

    val blobI: Iterator[Xor[TranslationError, (PhotoBlob, RawMetadataBlob)]] = translator.loadPhotoBlobs
    val graph = getGraph

    val results: Iterator[Xor[Error, Canonical]] = blobI.map { pairXor =>
      pairXor.flatMap { case (blob: PhotoBlob, raw: RawMetadataBlob) =>
          Ingress.addPhotoBlob(graph, blob, Some(raw))
      }
    }
    val errors: Iterator[Error] = results.collect { case Xor.Left(err) => err }
    val canonicals: Iterator[Canonical] = results.collect { case Xor.Right(c) => c }

    println(s"Import finished: ${canonicals.length} canonicals imported ${errors} errors reported (see below)")
    println(errors)
  }
}