package net.imadz.git.stats.algo

import scala.collection.mutable.ListBuffer

/**
 * Basic immutable undirected graph class
 *
 * @constructor Create a new [[Graph]]
 * @param V Number of vertices
 * @param E Number of edges
 * @param adj_list Edge adjacency lists
 */
class Graph(val V: Int, val E: Int,
    private val adj_list: IndexedSeq[List[UndirectedEdge]])
  extends UndirectedGraph[UndirectedEdge] {

  def degree(v: Int): Int = {
    require(v >= 0 & v < V, s"Specified vertex $v out of range [0, $V)")
    adj_list(v).length
  }

  def adj(v: Int) = {
    require(v >= 0 & v < V, s"Specified vertex $v out of range [0, $V)")
    adj_list(v)
  }

  /** Basic string representation */
  override def toString: String =
    f"Undirected graph with $V%d vertices and $E%d edges"
}

object Graph {
  /**
   * Build new immutable Graph from a list of edges
   *
   * @param edgeList List of edges specified as tuples of ints
   * @param allowDup Allow duplicate edges
   * @param allowSelf Allow self loops
   * @return A new [[Graph]]
   */
  def apply(edgeList: List[(Int, Int)], allowDup: Boolean = false,
    allowSelf: Boolean = false): Graph = {

    // Count number of vertices
    val V = edgeList.map(t => t._1 max t._2).max + 1

    // Build up adjacency list, removing duplicates
    //  and self loops if needed
    val adj_init = Array.fill(V)(ListBuffer.empty[UndirectedEdge])
    if (allowDup) {
      var nedge = edgeList.length
      if (allowSelf) {
        // Simple case -- just insert
        edgeList foreach {
          t =>
            {
              adj_init(t._1) += UndirectedEdge(t._1, t._2)
              if (t._1 != t._2) adj_init(t._2) += UndirectedEdge(t._2, t._1)
            }
        }
      } else {
        // Remove self edges
        edgeList foreach {
          t =>
            if (t._1 != t._2) {
              adj_init(t._1) += UndirectedEdge(t._1, t._2)
              adj_init(t._2) += UndirectedEdge(t._2, t._1)
            } else nedge -= 1
        }
      }
      new Graph(V, nedge, adj_init.map(_.toList).toIndexedSeq)
    } else {
      // Remove duplicates; sort edges so that 0,1 and 1,0 count as a dup
      val edgeSet =
        if (allowSelf)
          edgeList.map(t => (t._1 min t._2, t._1 max t._2)).toSet
        else
          edgeList.filter {
            t => t._1 != t._2
          }.map {
            t => (t._1 min t._2, t._1 max t._2)
          }.toSet

      edgeSet foreach {
        t =>
          {
            adj_init(t._1) += UndirectedEdge(t._1, t._2)
            if (t._1 != t._2) adj_init(t._2) += UndirectedEdge(t._2, t._1)
          }
      }
      new Graph(V, edgeSet.size, adj_init.map(_.toList).toIndexedSeq)
    }
  }
}