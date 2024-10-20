package snake;

public class PositionDistance implements Comparable<PositionDistance> {

  private int[] position;
  private int distance;

  public PositionDistance(int[] position, int distance) {
    this.position = position;
    this.distance = distance;
  }

  @Override
  public int compareTo(PositionDistance other) {
    int comparison = Integer.compare(this.distance, other.distance);
    return comparison;
  }

  public int[] getPosition() {
    return position;
  }
}
