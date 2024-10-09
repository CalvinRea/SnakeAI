package snake;

import java.io.*;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

public class Snek extends DevelopmentAgent {

  private final int[][] directions = new int[][] { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
  private final int numObstacles = 3;
  private final int numZombies = 3;

  private final int minDistFromEdge = 10;// range 0 to 24
  private final int maxDistFromEdge = 15;
  private final int timeLimit = 10_000_000;
  private final int maxIterations = 3396;

  private int iterations = 0;

  private Board board;
  private int[] fakeApplePos = { -1, -1 };

  public static void main(String[] args) {
    Snek agent = new Snek();
    args = new String[] { "-develop" };// TODO: remove this before submission
    start(agent, args);
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

  private int move() {
    int[] myHead = this.board.getMyHead();
    int move = -1;

    System.err.println("Snake Length:" + this.board.getLength());

    if (this.board.getLength() == 5) {
      Timer t = new Timer();
      t.start();
      Path myPath = aStarRateLimited(this.board.getMyHead(), this.board.getApplePos(),
          this.board.inflateAllHeads(0, 1));
      t.stop();
      System.err.println("adhd cost:"+t.getElapsedTimeMillis());
      move = myPath.move;
      if (move != -1) {
        System.err.println("adhd move");
        return move;
      }
    }

    Timer t = new Timer();
    t.start();
    move = isClosestToApple();
    System.err.println("Apple cost: " + t.getElapsedTimeMillis());
    t.reset();

    if (move != -1) {

      System.err.println("Apple move");

      int[] movePos = new int[] { myHead[0] + directions[move][0], myHead[1] + directions[move][1] };

      if (this.board.isUnavailable(movePos, this.board.getUnInflated())) {
        System.err.print("error invalid move");
      }
      return move;
    }

    move = survivalMove();
    if (move != -1) {
      int[] movePos = new int[] { myHead[0] + directions[move][0], myHead[1] + directions[move][1] };

      if (this.board.isUnavailable(movePos, this.board.getUnInflated())) {
        System.err.print("error invalid move");
      }
      return move;
    }

    System.err.println("No move found");
    move = 0;
    return move;
  }

  private Path aStarRateLimited(int[] start, int[] goal, boolean[][] playArea) {

    if (board.isUnavailable(goal, playArea)) {
      return new Path(Integer.MAX_VALUE, -1);
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
        return new Path(Integer.MAX_VALUE, -1);
      }

      Node currentNode = openQueue.poll();
      int currentKey = hashPosition(currentNode.position);

      if (closedSet.contains(currentKey)) {
        continue; // Skip already processed nodes
      }

      closedSet.add(currentKey);

      if (Arrays.equals(currentNode.position, goal)) {
        // Reconstruct path
        List<int[]> path = new ArrayList<>();
        Node node = currentNode;

        while (node.parent != null) {
          path.add(0, node.position);
          node = node.parent;
        }

        int pathLength = path.size();

        if (pathLength == 0) {
          // Already at the goal
          return new Path(0, -1);
        }

        int[] nextPos = path.get(0);
        int move = getClosestMove(nextPos, start);

        return new Path(pathLength, move);
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
        int tentativeGCost = currentNode.gCost + 1;

        Node neighborNode = openSet.get(neighborKey);
        if (neighborNode == null || tentativeGCost < neighborNode.gCost) {
          Node newNeighborNode = new Node(neighborPos, goal);
          newNeighborNode.gCost = tentativeGCost;
          newNeighborNode.fCost = newNeighborNode.gCost + newNeighborNode.hCost;
          newNeighborNode.parent = currentNode;

          openSet.put(neighborKey, newNeighborNode);
          openQueue.add(newNeighborNode);
        }
      }
    }

    return new Path(Integer.MAX_VALUE, -1);
  }

  private int isClosestToApple() {
    int[] goal = this.board.getApplePos();
    int[] myHeadPos = this.board.getMyHead();

    Path myPath = aStarRateLimited(myHeadPos, goal, this.board.getPossible());

    myPath.size += this.board.getEnemyInflation() + 2;// this is 6 for generation

    if (myPath.move == -1) {
      return -1;
    }

    int[] movePos = new int[] {
        this.board.getMyHead()[0] + directions[myPath.move][0],
        this.board.getMyHead()[1] + directions[myPath.move][1] };

    if (trapped(movePos, this.board.getPossible())) {
      System.err.println("apple move traps");
      return -1;
    }

    if (this.board.isUnavailable(movePos, this.board.getUnInflated())) {
      System.err.println("error invalid move");
    }

    for (int[] headPos : this.board.getEnemyHeads()) {
      Path enemyPath = aStarRateLimited(headPos, goal, this.board.getUnInflated());// has to be uninflated

      if (myPath.size > enemyPath.size) {
        return -1;
      }
    }

    return myPath.move;
  }

  private boolean trapped(int[] pos, boolean[][] playArea) {

    int[][] corners = { { 0, 0 }, { 0, 49 }, { 49, 0 }, { 49, 49 } };
    int[] freestCorner = { -1, -1 };
    int maxFree = -1;

    for (int[] corner : corners) {
      corner = getFirstAvailable(corner, playArea);
      int free = measureFreeSpace(corner, playArea);

      if (free > maxFree) {
        freestCorner = corner;
        maxFree = free;
      }
    }

    boolean trapped = aStarRateLimited(pos, freestCorner, playArea).move == -1;
    return trapped;
  }

  private int[] generateApple(boolean[][] playArea, Set<Integer> visited) {

    int x = -1;
    int y = -1;
    int[] apple = { x, y };

    boolean found = false;

    while (!found) {

      iterations++;
      if (iterations >= maxIterations) {
        break;
      }

      x = (int) (Math.random() * this.board.getWidth());
      y = (int) (Math.random() * this.board.getHeight());
      apple = new int[] { x, y };

      if (visited.contains(hashPosition(apple))) {
        System.err.println("Works");
        continue;
      }

      visited.add(hashPosition(apple));

      if (!this.board.isUnavailable(apple, playArea)) {
        if (!Arrays.equals(board.getMyHead(), apple)) {
          if (!this.board.outOfBounds(apple, minDistFromEdge)) {
            if (this.board.outOfBounds(apple, maxDistFromEdge)) {
              if (!this.board.isCloseHead(apple)) {
                found = true;
              }
            }
          }
        }
      }
    }

    if (!found) {
      return new int[] { -1, -1 };
    }

    return apple;
  }

  private int gaslight(boolean[][] playArea) {

    int[] myHead = this.board.getMyHead();

    if (trapped(myHead, playArea)) {
      return -1;
    }

    boolean found = true;
    Path fakePath = aStarRateLimited(myHead, fakeApplePos, playArea);

    Set<Integer> visited = new HashSet<>();
    iterations = 0;

    if (fakePath.move == -1 || fakeApplePos[0] == -1 || Arrays.equals(board.getMyHead(), fakeApplePos)
        || trapped(fakeApplePos, playArea) || this.board.isCloseHead(fakeApplePos)) {
      found = false;
    }

    final int max = 50;
    int i = 0;

    while (!found) {
      i++;

      if (i >= max) {
        return -1;
      }

      fakeApplePos = generateApple(playArea, visited);

      if (this.board.outOfBounds(fakeApplePos, 0)) {
        continue;
      }

      if (trapped(fakeApplePos, playArea)) {
        continue;
      }

      fakePath = aStarRateLimited(myHead, fakeApplePos, playArea);

      if (fakePath.move != -1) {
        found = true;
      }
    }

    return fakePath.move;
  }

  private int lastResort(boolean[][] playArea) {
    int[] myHead = this.board.getMyHead();
    int bestMove = -1;
    int maxScore = -1;

    for (int i = 0; i < directions.length; i++) {
      int[] newPos = { myHead[0] + directions[i][0], myHead[1] + directions[i][1] };

      if (!this.board.isUnavailable(newPos, playArea)) {
        int score = measureFreeSpace(newPos, playArea);

        if (score > maxScore) {
          maxScore = score;
          bestMove = i;
        }
      }
    }

    return bestMove;
  }

  private int measureFreeSpace(int[] start, boolean[][] playArea) {
    int freeSpace = 0;

    if (this.board.isUnavailable(start, playArea)) {
      return freeSpace;
    }

    Queue<int[]> toVisit = new LinkedList<>();
    Set<Integer> visited = new HashSet<>();

    toVisit.add(start);
    visited.add(hashPosition(start));

    while (!toVisit.isEmpty()) {
      int[] current = toVisit.poll();
      freeSpace++;

      for (int[] direction : this.directions) {

        int[] neighbor = { current[0] + direction[0], current[1] + direction[1] };
        int neighborHash = hashPosition(neighbor);

        if (!visited.contains(neighborHash)) {
          visited.add(neighborHash);

          if (!this.board.isUnavailable(neighbor, playArea)) {
            toVisit.add(neighbor);
          }
        }
      }
    }
    return freeSpace;
  }

  private int survivalMove() {

    int move = -1;
    Timer t = new Timer();

    t.start();
    move = gaslight(this.board.getPossible());
    System.err.println("gaslight cost: " + t.getElapsedTimeMillis());
    t.reset();

    if (move != -1) {
      System.err.println("gaslight possible");
      return move;
    }

    t.start();
    move = lastResort(this.board.inflateAllHeads(0, 1));
    System.err.println("last resort cost: " + t.getElapsedTimeMillis());
    t.reset();

    if (move != -1) {
      System.err.println("lastResort");
      return move;
    }

    t.start();
    move = lastResort(this.board.inflateAllHeads(0, 0));
    System.err.println("banzai cost: " + t.getElapsedTimeMillis());
    t.reset();

    if (move != -1) {
      System.err.println("BANZAI");
      return move;
    }
    return -1;
  }

  /* UTILITY METHODS */

  private int manDist(int[] pos1, int[] pos2) {
    return Math.abs(pos1[0] - pos2[0]) + Math.abs(pos1[1] - pos2[1]);
  }

  private int getClosestMove(int[] goal, int[] current) {
    int move = -1;

    if (goal[1] < current[1]) {
      move = 0;
    } else if (goal[1] > current[1]) {
      move = 1;
    } else if (goal[0] < current[0]) {
      move = 2;
    } else if (goal[0] > current[0]) {
      move = 3;
    }

    if (move == -1) {
      System.err.println("goal == current?");
    }
    return move;
  }

  private int hashPosition(int[] position) {
    return position[1] * board.getWidth() + position[0];
  }

  private int[] unhashPosition(int hash) {
    int x = hash % board.getWidth();
    int y = hash / board.getWidth();
    return new int[] { x, y };
  }

  private int[] getFirstAvailable(int[] pos, boolean[][] playArea) {

    Queue<int[]> toVisit = new LinkedList<>();
    Set<Integer> visited = new HashSet<>();

    if (!this.board.isUnavailable(pos, playArea)) {
      return pos;
    }
    toVisit.add(pos);
    int posHash = hashPosition(pos);
    visited.add(posHash);

    while (!toVisit.isEmpty()) {
      int[] current = toVisit.poll();

      for (int i = 0; i < directions.length; i++) {
        int[] direction = directions[i];
        int[] neighbor = { current[0] + direction[0], current[1] + direction[1] };
        int neighborHash = hashPosition(neighbor);

        if (!visited.contains(neighborHash)) {
          visited.add(neighborHash);

          if (!this.board.isUnavailable(neighbor, playArea)) {
            return neighbor;
          } else {
            toVisit.add(neighbor);
          }
        }
      }
    }

    return new int[] { -1, -1 };
  }

}