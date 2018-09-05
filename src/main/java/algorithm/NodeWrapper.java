package algorithm;

public class NodeWrapper implements Comparable {

  public double mDistance;
  public double mCost;
  public double mPotential;
  public final int mNodeID;
  public int mParent;
  public int mPreviousEdgeID;

  public NodeWrapper(int mID, double cost, int parent, int previousEdgeID, double distance) {
    mNodeID = mID;
    mDistance = distance;
    mParent = parent;
    mPreviousEdgeID = previousEdgeID;
    mCost = cost;
  }

  public int compareTo(Object o) {
    NodeWrapper n = (NodeWrapper) o;
    if (mCost == n.mCost) {
      return 0;
    }
    if (mCost < n.mCost) {
      return -1;
    }
    return 1;
  }
}
