import java.util.Iterator;

public abstract class EdgeIter implements Iterator<Boolean> {

    /**
     * Deprecated. Do not use. Always returns false
     * @return false
     */
    @Deprecated
    @Override
    public abstract boolean hasNext();

    /**
     * Returns true is there are more edges.
     * If returns true, then also iterate one step
     * @return true if hasNext returns true and proceed one step. False otherwise
     */
    @Override
    public abstract Boolean next();

    /**
     * Get the cost of the current iterated edge. Note the cost could be
     * time, or distance or both.
     * @return the COST of the edge
     */
    public abstract double getCost();

    /**
     * Get the geological distance of the edge.
     * @return the GEOLOGICAL distance of the edge
     */
    public abstract double getDistance();

    /**
     * Get the id of the next node
     * @return id of the next node
     */
    public abstract int getNext();

    /**
     * Will be helpful to compute the weight
     * @return the current edge id
     */
    public abstract int getEdge();

}
