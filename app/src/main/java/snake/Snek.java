package snake;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import za.ac.wits.snake.DevelopmentAgent;

public class Snek extends DevelopmentAgent {
  private final int[][] directions = new int[][] { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };

  private final int timeLimit = 20000000;

  private final int numObstacles = 3;

  private final int numZombies = 3;

  private Board board;

  private int cornerVisiting = 0;

  public static void main(String[] args) {
    Snek agent = new Snek();
    start(agent, args);
  }

  private int getMinSafe() {
    return 6;
  }

  private int calculateMaxLevel() {
    int snakeLength = this.board.getLength();
    if (snakeLength < 25)
      return 5;
    return 100;
  }

  public void run() {
    try {
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      try {
        String initString = br.readLine();
        String[] temp = initString.split(" ");
        int numSnakes = Integer.parseInt(temp[0]);
        int width = Integer.parseInt(temp[1]);
        int height = Integer.parseInt(temp[2]);
        System.err.println("New game");
        while (true) {
          HashMap<String, ArrayList<String>> lines = new HashMap<>();
          lines.put("appleLine", new ArrayList<>(1));
          lines.put("obstacleLines", new ArrayList<>(3));
          lines.put("zombieLines", new ArrayList<>(3));
          lines.put("mySnakeNumLine", new ArrayList<>(1));
          lines.put("snakeLines", new ArrayList<>());
          String appleLine = br.readLine();
          if (appleLine.contains("Game Over")) {
            System.err.println("game over");
            break;
          }
          boolean alive = true;
          ((ArrayList<String>) lines.get("appleLine")).add(appleLine);
          for (int obstacle = 0; obstacle < 3; obstacle++)
            ((ArrayList<String>) lines.get("obstacleLines")).add(br.readLine());
          for (int zombie = 0; zombie < 3; zombie++)
            ((ArrayList<String>) lines.get("zombieLines")).add(br.readLine());
          ((ArrayList<String>) lines.get("mySnakeNumLine")).add(br.readLine());
          int mySnakeNum = Integer.parseInt(((ArrayList<String>) lines.get("mySnakeNumLine")).get(0));
          int myHeadNum = -1;
          int deadSnakes = 0;
          for (int i = 0; i < numSnakes; i++) {
            String snakeLine = br.readLine();
            if (snakeLine.charAt(0) == 'd') {
              if (i == mySnakeNum)
                alive = false;
              deadSnakes++;
            } else {
              ((ArrayList<String>) lines.get("snakeLines")).add(snakeLine);
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
        br.close();
      } catch (Throwable throwable) {
        try {
          br.close();
        } catch (Throwable throwable1) {
          throwable.addSuppressed(throwable1);
        }
        throw throwable;
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
        if (corners[i][0] != center[0] && corners[i][1] != center[1])
          break;
      }
    }
    if (manDist(corners[this.cornerVisiting], myHead) < 5)
      this.cornerVisiting = (this.cornerVisiting + 1) % corners.length;
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
    if (move != -1)
      return move;
    move = survivalMove();
    if (move != -1)
      return move;
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
    if (current.parent != null)
      return new int[][] { current.position, current.parent.position, { pathLength } };
    return new int[][] { current.position, current.position, { pathLength } };
  }

  private Node aStarRateLimited(int[] start, int[] goal, boolean[][] playArea) {
    if (this.board.isUnavailable(goal, playArea))
      return null;
    int[][] moves = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    Node startNode = new Node(start, goal);
    startNode.gCost = 0;
    startNode.fCost = startNode.hCost;
    PriorityQueue<Node> toVisit = new PriorityQueue<>();
    HashMap<String, Node> visited = new HashMap<>();
    toVisit.add(startNode);
    long startTime = System.nanoTime();
    while (!toVisit.isEmpty()) {
      if (System.nanoTime() - startTime >= 20000000L)
        return null;
      Node currNode = toVisit.poll();
      String currKey = currNode.position[0] + "," + currNode.position[1];
      if (visited.containsKey(currKey))
        continue;
      visited.put(currKey, currNode);
      if (Arrays.equals(currNode.position, goal))
        return currNode;
      for (int[] move : moves) {
        int[] nPos = { currNode.position[0] + move[0], currNode.position[1] + move[1] };
        String nPosKey = nPos[0] + "," + nPos[1];
        if (!this.board.isUnavailable(nPos, playArea) && !visited.containsKey(nPosKey)) {
          Node moveNode = new Node(nPos, goal);
          moveNode.updateCosts(currNode);
          boolean shouldAdd = true;
          for (Node node : toVisit) {
            if (Arrays.equals(node.position, nPos) && node.gCost <= moveNode.gCost) {
              shouldAdd = false;
              break;
            }
          }
          if (shouldAdd)
            toVisit.add(moveNode);
        }
      }
    }
    return null;
  }

  private int isClosestToApple() {
    int[] goal = this.board.getApplePos();
    int[] myHeadPos = this.board.getMyHead();
    Node myNode = aStarRateLimited(myHeadPos, goal, this.board.getPossible());
    if (myNode == null)
      return -1;
    int[][] myData = backtrackSize(myNode);
    int[] closestHeadChild = myData[0];
    int[] closestHead = myData[1];
    int minLength = myData[2][0] + 2;
    for (int[] headPos : this.board.getEnemyHeads()) {
      Node n = aStarRateLimited(headPos, goal, this.board.getUnInflated());
      if (n == null)
        continue;
      int[][] data = backtrackSize(n);
      if (data[2][0] < minLength) {
        closestHeadChild = data[0];
        closestHead = data[1];
        minLength = data[2][0];
        break;
      }
    }
    if (closestHead == null)
      return -1;
    if (Arrays.equals(closestHead, myHeadPos) && isAppleSafe(goal))
      return getClosestMove(closestHeadChild, closestHead);
    return -1;
  }

  private boolean isAppleSafe(int[] goal) {
    int blocked = 0;
    int minSafe = getMinSafe();
    for (int i = -1; i < 2; i++) {
      for (int j = -1; j < 2; j++) {
        int[] newPos = { goal[0] + i, goal[1] + j };
        if (this.board.isUnavailable(newPos, this.board.getPossible()))
          blocked++;
      }
    }
    return (blocked <= minSafe);
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
    if (this.board.isUnavailable(startPos, playArea))
      return 0.0D;
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