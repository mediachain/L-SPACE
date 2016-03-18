package io.mediachain.util

import cats.Functor
import cats.data.Xor
import cats.free.Free
import gremlin.scala._
import io.mediachain.GraphError
import io.mediachain.GraphError.TransactionFailed

object TransactionMonad {
  case class GraphOp[A](action: Graph => A)

  implicit object GraphOpFunctor extends Functor[GraphOp] {
    def map[A, B](a: GraphOp[A])(g: A => B) =
      GraphOp[B]((graph: Graph) => g(a.action(graph)))
  }

  type Transaction[A] = Free[GraphOp, A]

  def runTransactionImpl[A](graph: Graph, tx: Transaction[A]): A = tx.fold (
    (a: A) => a,
    (x: GraphOp[Transaction[A]]) => runTransactionImpl(graph, x.action(graph))
  )

  def runTransaction[A](graph: Graph, trans: Transaction[A])
  : Xor[GraphError, A] = {
    val result: Xor[GraphError, A] =
      Xor.catchNonFatal {
        graph.tx.open()
        runTransactionImpl(graph, trans)
      }.leftMap(TransactionFailed)

    result match {
      case Xor.Left(_) => graph.tx.rollback()
      case _ => graph.tx.commit()
    }

    result
  }
}

object TxMonadExample {
  import io.mediachain.util.TransactionMonad._

  def addVertexWithLabel(foo: String) = Free.liftF(
    GraphOp((graph: Graph) => {
      graph + foo
    })
  )

  def setFooPropToTrue(v: Vertex) = Free.liftF(
    GraphOp((graph: Graph) => {
      val key = Key[Boolean]("foo")
      v.setProperty(key, true)
    })
  )

  def foo(graph: Graph): Xor[GraphError, Vertex] = {
    val combinedOps = for {
      bar <- addVertexWithLabel("bar")
      barWithProp <- setFooPropToTrue(bar)
    } yield barWithProp

    runTransaction(graph, combinedOps)
  }
}