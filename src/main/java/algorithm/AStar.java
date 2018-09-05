package algorithm;

public class AStar extends Dijkstra {

  public AStar(CallBacks callBacks) {
    super(callBacks);
  }

  @Override
  protected Integer[] reconstructPath(NodeWrapper current) {
    Integer[] points = super.reconstructPath(current);
    /* Reevaluate the potential */
    NodeWrapper[] tempQueue = new NodeWrapper[mQueue.size()];
    mQueue.toArray(tempQueue);
    mQueue.clear();
    for (NodeWrapper nodeWrapper : tempQueue) {
      double newPotential = mCallBacks.getPotential(nodeWrapper.mNodeID, mTargets);
      nodeWrapper.mCost -= nodeWrapper.mPotential;
      nodeWrapper.mCost += newPotential;
      nodeWrapper.mPotential = newPotential;
      mQueue.add(nodeWrapper);
    }
    return points;
  }
}
