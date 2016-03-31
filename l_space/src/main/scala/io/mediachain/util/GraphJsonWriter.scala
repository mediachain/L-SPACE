package io.mediachain.util

import java.io._
import java.util

import gremlin.scala._
import org.apache.tinkerpop.gremlin.structure.Direction
import org.apache.tinkerpop.gremlin.structure.io.IoCore
import org.json4s._
import org.json4s.jackson.{Serialization, JsonMethods => Json}
import org.json4s.jackson.Serialization.{write => JsonWrite}
import scala.collection.JavaConverters._

import scala.collection.JavaConversions._

object GraphJsonWriter {

  class EmbeddedMapSerializer extends
    CustomSerializer[util.Map[_, _]](format => (
      {
        case jObject@JObject(_) => jObject.values.asJava
      },
      {
        case m: util.Map[_, _] =>
          JObject(
            m.entrySet().map { entry =>
              val key = entry.getKey.toString
              val stringVal = entry.getValue.toString
              JField(key, JString(stringVal))
            }.toList
          )
      }
      ))

  case class D3Node(label: String, id: String, properties: Map[String, Any])

  case class D3Link(label: String, id: String, properties: Map[String, Any],
    source: String, target: String)

  case class D3Graph(nodes: Map[String, D3Node], links: List[D3Link])

  def makeD3Graph(graph: Graph): D3Graph = {
    val nodes: Map[String, D3Node] = graph.V.map(_.asD3Node)
      .map(n => n.id -> n)
      .toMap

    val edges: List[D3Link] = graph.E.map(_.asD3Link).toList

    D3Graph(nodes, edges)
  }

  def graphToD3JsonString(graph: Graph): String = {
    val d3Graph = makeD3Graph(graph)
    val formats = Serialization.formats(NoTypeHints) + new EmbeddedMapSerializer
    JsonWrite(d3Graph)(formats)
  }

  case class CytoElement( group: String, classes: String, data: Map[String, Any])

  def graphToCytoscapeJsonString(graph: Graph): String = {
    val elements: List[CytoElement] =
      graph.V.map(_.asCytoscapeElement).toList ++
        graph.E.map(_.asCytoscapeElement).toList

    val formats = Serialization.formats(NoTypeHints) +
      new EmbeddedMapSerializer

    JsonWrite(elements)(formats)
  }


  implicit class GraphIOImplicits(graph: Graph) {
    lazy val writer = graph.io(IoCore.graphson).writer.create

    def toGraphsonString: String = {
      val out = new ByteArrayOutputStream
      writer.writeGraph(out, graph)
      out.toString("UTF-8")
    }

    def printGraphson(): Unit = {
      writer.writeGraph(System.out, graph)
    }

    def toD3JsonString: String =
      graphToD3JsonString(graph)

    def printD3JsonString(): Unit = {
      println(toD3JsonString)
    }

    def toCytoscapeJsonString: String =
      graphToCytoscapeJsonString(graph)

  }

  implicit class VertexIOImplicits(vertex: Vertex) {
    lazy val writer = vertex.graph.io(IoCore.graphson).writer.create

    def toGraphsonString(edgeDirection: Direction = Direction.BOTH): String = {
      val out = new ByteArrayOutputStream
      writer.writeVertex(out, vertex, edgeDirection)
      out.toString("UTF-8")
    }

    def printGraphson(edgeDirection: Direction = Direction.BOTH): Unit = {
      writer.writeVertex(System.out, vertex, edgeDirection)
    }

    def asCytoscapeElement: CytoElement = {
      val id = vertex.id.toString
      val data = vertex.valueMap[Any].filterKeys(_ != "__gs") +
        ("label" -> vertex.label) + ("id" -> id)
      CytoElement("nodes", vertex.label, data)
    }

    def asD3Node: D3Node = {
      val id = vertex.id.toString
      D3Node(vertex.label, id, vertex.valueMap.filterKeys(_ != "__gs"))
    }
  }

  implicit class EdgeIOImplicits(edge: Edge) {
    lazy val writer = edge.graph.io(IoCore.graphson).writer.create

    def toGraphsonString: String = {
      val out = new ByteArrayOutputStream
      writer.writeEdge(out, edge)
      out.toString("UTF-8")
    }

    def printGraphson(): Unit = {
      writer.writeEdge(System.out, edge)
    }

    def asCytoscapeElement: CytoElement = {
      val id = edge.id.toString
      val sourceId = edge.outVertex.id.toString
      val targetId = edge.inVertex.id.toString
      val data = edge.valueMap[Any].filterKeys(_ != "__gs") +
        ("label" -> edge.label) + ("id" -> id) +
        ("source" -> sourceId) + ("target" -> targetId)
      CytoElement("edges", edge.label, data)
    }

    def asD3Link: D3Link = {
      val sourceId = edge.outVertex().id.toString
      val targetId = edge.inVertex().id.toString

      D3Link(edge.label, edge.id.toString,
        edge.valueMap.filterKeys(_ != "__gs"),
        sourceId, targetId)
    }
  }

  // TOOD: error handling
  def toGraphsonVertexObjects(graph: Graph): Iterator[JObject] = {
    val writer = graph.io(IoCore.graphson).writer.create
    val verts: Iterator[Vertex] = graph.V.toStream.iterator

    verts.map { v =>
      val out = new ByteArrayOutputStream()
      writer.writeVertex(out, v, Direction.BOTH)
      val in = new ByteArrayInputStream(out.toByteArray)
      Json.parse(in).asInstanceOf[JObject]
    }
  }
}
