package snake;

public class Node implements Comparable<Node> {
  // implements Comparable is used for priority queue

  public int fCost, gCost, hCost; // total Cost, actual Cost, heuristic Cost
  public int[] position;
  Node parent;

  public Node(int[] inPos, int[] goalPos) {
    position = inPos;
    this.hCost = distanceTo(inPos, goalPos);
    this.gCost = Integer.MAX_VALUE;
    this.fCost = this.gCost + this.hCost;
    this.parent = null;
  }

  public void updateCosts(Node parent) {
    this.gCost = parent.gCost + 1;
    this.fCost = this.gCost + this.hCost;
    this.parent = parent;
  }
  // Heuristic Function for manhattan distance, h value
  // if lacking speed multiply this Function by a constant say 10
  // this will give weighted a star, it will be much faster but slightly
  // inefficient credit to D. Chris Rayner. I thought the performance gain wasn't worth it.
  public int distanceTo(int[] a, int[] b) {
    return (Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]));
  }

  @Override public int compareTo(Node other) { // for priority queue
    int comparison = Integer.compare(this.fCost, other.fCost);
    
    if (comparison == 0) {
      // break ties in favor of depth so larger g cost credit to Nathan
      // Sturtevant it made A* run 20% faster :)
      return Integer.compare(other.gCost, this.gCost);
    }
    return comparison;
  }
}
