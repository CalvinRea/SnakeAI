package snake;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

public class Snek extends DevelopmentAgent {
  private final int[][] directions = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

  private final int timeLimit = 20_000_000;

  private final int numObstacles = 3;

  private final int numZombies = 3;

  private Board board;

  private int cornerVisiting = 0;

  public static void main(String[] args) {
    Snek agent = new Snek();
    start(agent, args);
  }

  private int calculateMaxLevel() {
    int snakeLength = this.board.getLength();
    if (snakeLength < 25)
      return 5;
    return 100;
  }

  public void run() {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {

        String initString = br.readLine();
        String[] temp = initString.split(" ");
        int numSnakes = Integer.parseInt(temp[0]);
        int width = Integer.parseInt(temp[1]);
        int height = Integer.parseInt(temp[2]);
        System.err.println("New game");
        while (true) {
          HashMap<String, ArrayList<String>> lines = new HashMap<>();
          lines.put("appleLine", new ArrayList<>());
          lines.put("obstacleLines", new ArrayList<>());
          lines.put("zombieLines", new ArrayList<>());
          lines.put("mySnakeNumLine", new ArrayList<>());
          lines.put("snakeLines", new ArrayList<>());
          String appleLine = br.readLine();
          if (appleLine.contains("Game Over")) {
            System.err.println("game over");
            break;
          }
          boolean alive = true;

        lines.get("appleLine").add(appleLine);

        for (int obstacle = 0; obstacle < numObstacles; obstacle++) {
          lines.get("obstacleLines").add(br.readLine());
        }

        for (int zombie = 0; zombie < numZombies; zombie++) {
          lines.get("zombieLines").add(br.readLine());
        }

        lines.get("mySnakeNumLine").add(br.readLine());

        int mySnakeNum = Integer.parseInt(lines.get("mySnakeNumLine").get(0));
          int myHeadNum = -1;
          int deadSnakes = 0;
          for (int i = 0; i < numSnakes; i++) {
            String snakeLine = br.readLine();
            if (snakeLine.charAt(0) == 'd') {
            if (i == mySnakeNum) {
                alive = false;
            }
              deadSnakes++;
            } else {
              lines.get("snakeLines").add(snakeLine);
              if (i == mySnakeNum)
                myHeadNum = i - deadSnakes;
            }
          }
          if (alive) {
            this.board = new Board(lines, width, height, myHeadNum);
            int move = move();
            System.out.println(move);
            continue;
          }
          System.err.println("not alive");
        }

      } catch (Exception e) {
    e.printStackTrace();
  }
  }

  private int getCornerMove() {
    int[] myHead = this.board.getMyHead();
    int move = -1;
    int[][] corners = { { 10, 10 }, { 40, 40 } };
    int[] center = { 25, 25 };
    for (int i = 0; i < corners.length; i++) {
      while (this.board.isUnavailable(corners[i], this.board.getPossible())) {
        corners[i][0] = corners[i][0] + ((corners[i][0] < center[0]) ? 1 : -1);
        corners[i][1] = corners[i][1] + ((corners[i][1] < center[1]) ? 1 : -1);
        if (corners[i][0] != center[0] && corners[i][1] != center[1]) {
          break;
        }
      }
    }
    if (manDist(corners[this.cornerVisiting], myHead) < 5) {
      this.cornerVisiting = (this.cornerVisiting + 1) % corners.length;
    }
    Node n = aStarRateLimited(myHead, corners[this.cornerVisiting], this.board.getPossible());
    if (n == null) {
      this.cornerVisiting = (this.cornerVisiting + 1) % corners.length;
      n = aStarRateLimited(myHead, corners[this.cornerVisiting], this.board.getPossible());
    }
    if (n != null) {
      int[][] pathData = backtrackSize(n);
      return getClosestMove(pathData[0], pathData[1]);
    }
    return move;
  }

  private int move() {
    int move = -1;
    move = isClosestToApple();
    if (move != -1) {
      return move;
    }

    move = survivalMove();
    if (move != -1) {
      return move;
    }
    move = 0;
    return move;
  }

  private int[][] backtrackSize(Node end) {
    Node current = end;
    int pathLength = 1;
    while (current.parent != null && current.parent.parent != null) {
      current = current.parent;
      pathLength++;
    }
    if (current.parent != null) {
      return new int[][] { current.position, current.parent.position, { pathLength } };
    }
    return new int[][] { current.position, current.position, { pathLength } };
  }

  private Node aStarRateLimited(int[] start, int[] goal, boolean[][] playArea) {
    if (board.isUnavailable(goal, playArea)) {
      return null;
    }

    int[][] moves = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    Node startNode = new Node(start, goal);
    startNode.gCost = 0;
    startNode.fCost = startNode.hCost;

    PriorityQueue<Node> openQueue = new PriorityQueue<>();
    Map<Integer, Node> openSet = new HashMap<>();
    Set<Integer> closedSet = new HashSet<>();

    int startKey = hashPosition(startNode.position);
    openQueue.add(startNode);
    openSet.put(startKey, startNode);

    long startTime = System.nanoTime();

    while (!openQueue.isEmpty()) {

      if (System.nanoTime() - startTime >= timeLimit) {
        return null;
      }

      Node currentNode = openQueue.poll();
      int currentKey = hashPosition(currentNode.position);

      openSet.remove(currentKey);
      closedSet.add(currentKey);

      if (Arrays.equals(currentNode.position, goal)) {
        return currentNode;
      }

      for (int[] move : moves) {
        int[] neighborPos = {
            currentNode.position[0] + move[0],
            currentNode.position[1] + move[1]
        };

        if (board.isUnavailable(neighborPos, playArea)) {
          continue;
        }

        int neighborKey = hashPosition(neighborPos);
        if (closedSet.contains(neighborKey)) {
          continue;
        }

        int tentativeGCost = currentNode.gCost + 1;

        Node neighborNode = openSet.get(neighborKey);
        if (neighborNode == null) {

          neighborNode = new Node(neighborPos, goal);
          neighborNode.gCost = tentativeGCost;
          neighborNode.fCost = neighborNode.gCost + neighborNode.hCost;
          neighborNode.parent = currentNode;

          openSet.put(neighborKey, neighborNode);
          openQueue.add(neighborNode);
        } else if (tentativeGCost < neighborNode.gCost) {

          neighborNode.gCost = tentativeGCost;
          neighborNode.fCost = neighborNode.gCost + neighborNode.hCost;
          neighborNode.parent = currentNode;

          openQueue.remove(neighborNode);
          openQueue.add(neighborNode);
        }
      }
    }

    return null;
  }

  private int hashPosition(int[] position) {
    return position[0] * board.getWidth() + position[1];
  }

  private int isClosestToApple() {
    int[] goal = this.board.getApplePos();
    int[] myHeadPos = this.board.getMyHead();
    Node myNode = aStarRateLimited(myHeadPos, goal, this.board.getPossible());
    if (myNode == null) {
      return -1;
    }

    int[][] myData = backtrackSize(myNode);
    int[] closestHeadChild = myData[0];
    int[] closestHead = myData[1];
    int minLength = myData[2][0] + 2;
    for (int[] headPos : this.board.getEnemyHeads()) {
      Node n = aStarRateLimited(headPos, goal, this.board.getUnInflated());
      if (n == null) {
        continue;
      }

      int[][] data = backtrackSize(n);

      if (data[2][0] < minLength) {
        closestHeadChild = data[0];
        closestHead = data[1];
        minLength = data[2][0];
        break;
      }
    }

    if (closestHead == null) {
      return -1;
    }
    if (Arrays.equals(closestHead, myHeadPos) && isAppleSafe(goal)) {
      return getClosestMove(closestHeadChild, closestHead);
    }
    return -1;
  }

  private boolean isAppleSafe(int[] goal) {

    int snakeLength = this.board.getLength();
    if (snakeLength < 25) {
      return true;
    }

    int gap = 1;
    if (goal[0] < 0 + gap || goal[1] < 0 + gap || goal[0] > 49 - gap || goal[1] > 49 - gap) {
      // if space between apple and wall is not 1 return false
      return false;
    }

    // calculate blocked spaces
    int maxBlocked = 3;// lower bound, anything less won't eat the apple
    int blocked = 0;

    for (int i = -1; i < 2; i++) {
      for (int j = -1; j < 2; j++) {
        int[] newPos = new int[] { goal[0] + i, goal[1] + j };
        if (this.board.isUnavailable(newPos, this.board.getPossible())) {
          blocked++;
        }
      }
    }
    System.err.println(" blocked: " + blocked + " maxBlocked: " + maxBlocked);
    return blocked < maxBlocked;
  }

  private int survivalMove() {
    int move = -1;
    int length = this.board.getLength();
    if (length > 23) {
      move = getCornerMove();
      if (move != -1)
        return move;
    }
    int[] myHead = this.board.getMyHead();
    PriorityQueue<PositionScore> maxHeap = new PriorityQueue<>((p1, p2) -> Double.compare(p2.score, p1.score));
    int maxLevel = calculateMaxLevel();
    for (int[] d : this.directions) {
      int[] newPos = { myHead[0] + d[0], myHead[1] + d[1] };
      if (!this.board.isUnavailable(newPos, this.board.getPossible())) {
        double score = measureFreeSpace(newPos, this.board.getPossible(), maxLevel);
        maxHeap.add(new PositionScore(newPos, score));
      }
    }
    if (maxHeap.size() != 0) {
      PositionScore best = maxHeap.poll();
      return getClosestMove(best.position, myHead);
    }
    maxHeap = new PriorityQueue<>((p1, p2) -> Double.compare(p2.score, p1.score));
    boolean[][] unInflated = this.board.getUnInflated();
    for (int[] d : this.directions) {
      int[] newPos = { myHead[0] + d[0], myHead[1] + d[1] };
      if (!this.board.isUnavailable(newPos, unInflated)) {
        double score = measureFreeSpace(newPos, unInflated, maxLevel);
        maxHeap.add(new PositionScore(newPos, score));
      }
    }
    if (maxHeap.size() != 0) {
      PositionScore best = maxHeap.poll();
      return getClosestMove(best.position, myHead);
    }
    return move;
  }

  private double measureFreeSpace(int[] startPos, boolean[][] playArea, int maxLevel) {
    if (this.board.isUnavailable(startPos, playArea)) {
      return 0;
    }

    boolean[][] visited = new boolean[50][50];
    Queue<int[]> queue = new LinkedList<>();
    queue.add(startPos);
    visited[startPos[0]][startPos[1]] = true;
    double freeSpace = 0.0D;
    while (!queue.isEmpty()) {
      int[] currentPos = queue.poll();
      int manDist = manDist(startPos, currentPos);
      if (manDist > maxLevel)
        continue;
      freeSpace++;
      for (int[] direction : this.directions) {
        int[] newPos = { currentPos[0] + direction[0], currentPos[1] + direction[1] };
        if (!this.board.isUnavailable(newPos, playArea) && !visited[newPos[0]][newPos[1]]) {
          visited[newPos[0]][newPos[1]] = true;
          queue.add(newPos);
        }
      }
    }
    return freeSpace;
  }

  /* UTILITY METHODS */

  private int getClosestMove(int[] goal, int[] current) {
    if (goal[0] != current[0]) {
      if (goal[0] > current[0])
        return 3;
      return 2;
    }
    if (goal[1] > current[1])
      return 1;
    return 0;
  }

  private int manDist(int[] pos1, int[] pos2) {
    return Math.abs(pos1[0] - pos2[0]) + Math.abs(pos1[1] - pos2[1]);
  }
}