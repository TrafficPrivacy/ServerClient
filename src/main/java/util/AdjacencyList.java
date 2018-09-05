package util;

import java.util.ArrayList;
import java.util.HashMap;

public class AdjacencyList<Node> {

  private HashMap<Node, ArrayList<Node>> mTable;
  private HashMap<Pair<Node, Node>, Double> mWeight;
  private HashMap<Pair<Node, Node>, Double> mDistance;

  public AdjacencyList() {
    mTable = new HashMap<>();
    mWeight = new HashMap<>();
    mDistance = new HashMap<>();
  }

  public AdjacencyList<Node> insertNode(Node n) {
    if (!mTable.containsKey(n)) {
      mTable.put(n, new ArrayList<>());
    }
    return this;
  }

  public AdjacencyList<Node> insertEdge(Node from, Node to) {
    if (!mTable.containsKey(from)) {
      insertNode(from);
    }
    if (!mTable.containsKey(to)) {
      insertNode(to);
    }
    if (!mTable.get(from).contains(to)) {
      mTable.get(from).add(to);
    }
    return this;
  }

  public AdjacencyList<Node> setWeight(Node from, Node to, Double weight) {
    mWeight.put(new Pair<>(from, to), weight);
    return this;
  }

  public AdjacencyList<Node> setDistance(Node from, Node to, Double distance) {
    mDistance.put(new Pair<>(from, to), distance);
    return this;
  }

  public Double getWeight(Node from, Node to) {
    return mWeight.get(new Pair<>(from, to));
  }

  public Double getDistance(Node from, Node to) {
    return mDistance.get(new Pair<>(from, to));
  }

  public AdjacencyList<Node> removeNode(Node n) {
    if (mTable.containsKey(n)) {
      mTable.remove(n);
      for (Node object : mTable.keySet()) {
        ArrayList<Node> list = mTable.get(object);
        list.remove(n);
      }
    }
    return this;
  }

  public ArrayList<Node> getNeighbors(Node n) {
    if (mTable.containsKey(n)) {
      return mTable.get(n);
    } else {
      return null;
    }
  }

  public boolean hasNode(Node n) {
    return mTable.containsKey(n);
  }

  public ArrayList<Node> getNodes() {
    return new ArrayList<Node>(mTable.keySet());
  }
}

