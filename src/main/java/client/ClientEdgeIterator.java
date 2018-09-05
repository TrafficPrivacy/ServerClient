package client;

import algorithm.EdgeIterator;
import util.AdjacencyList;

import java.util.ArrayList;

public class ClientEdgeIterator extends EdgeIterator {

  private AdjacencyList<Integer> mGraph;
  private ArrayList<Integer> mNeighbors;
  private int mCurrent;
  private int mCurIdx;

  public ClientEdgeIterator(int current, AdjacencyList<Integer> graph) {
    mGraph = graph;
    mCurrent = current;
    mCurIdx = -1;
    mNeighbors = graph.getNeighbors(current);
  }

  /**
   * Don't use
   *
   * @return always false
   */
  @Deprecated
  @Override
  public boolean hasNext() {
    return false;
  }

  @Override
  public Boolean next() {
    return (mNeighbors != null) && (++mCurIdx < mNeighbors.size());
  }

  @Override
  public double getCost() {
    if (mNeighbors != null) {
      return mGraph.getDistance(mCurrent, mNeighbors.get(mCurIdx));
    }
    return -1;
  }

  @Override
  public double getDistance() {
    if (mNeighbors != null) {
      return mGraph.getDistance(mCurrent, mNeighbors.get(mCurIdx));
    }
    return -1;
  }

  @Override
  public int getNext() {
    if (mNeighbors != null) {
      return mNeighbors.get(mCurIdx);
    }
    return -1;
  }

  @Override
  public int getEdge() {
    return 0;
  }
}
