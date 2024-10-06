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

  private Board board;
  private int[] fakeApplePos = { -1, -1 };

  public static void main(String[] args) {
    Snek agent = new Snek();
    args = new String[] { "-develop" };
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
    move = isClosestToApple();
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

  private Path bfs(int[] start, int[] goal, boolean[][] playArea) {

    Path path = new Path(-1, -1);

    if (Arrays.equals(start, goal)) {
      return path;
    }

    if (this.board.isUnavailable(goal, playArea)) {
      return path;
    }

    Queue<int[]> toVisit = new LinkedList<>();
    Set<Integer> visited = new HashSet<>();
    Map<Integer, Integer> parentMap = new HashMap<>();

    toVisit.add(start);
    visited.add(hashPosition(start));

    boolean found = false;
    int[] endPos = null;

    while (!toVisit.isEmpty()) {
      int[] current = toVisit.poll();

      if (Arrays.equals(current, goal)) {
        endPos = current;
        found = true;
        break;
      }

      for (int i = 0; i < directions.length; i++) {
        int[] direction = directions[i];
        int[] neighbor = { current[0] + direction[0], current[1] + direction[1] };
        int neighborHash = hashPosition(neighbor);
        if (!this.board.isUnavailable(neighbor, playArea)) {
          if (!visited.contains(neighborHash)) {
            parentMap.put(neighborHash, hashPosition(current));
            toVisit.add(neighbor);
            visited.add(neighborHash);
          }
        }
      }
    }

    int size = -1;
    int move = -1;

    if (found) {

      List<int[]> pathList = new LinkedList<>();
      int[] tempPos = endPos;
      int tempHash = hashPosition(tempPos);

      // backtrack from goal to start
      while (!Arrays.equals(tempPos, start)) {
        pathList.addFirst(tempPos);
        int parentHash = parentMap.get(tempHash);
        tempPos = unhashPosition(parentHash);
        tempHash = parentHash;
      }

      if (pathList.size() > 0) {
        int[] nextPos = pathList.getFirst();
        move = getClosestMove(nextPos, start);
        size = pathList.size();
        path = new Path(size, move);

      } else {
        // start is next to the goal
        move = getClosestMove(goal, start);
        size = 1;
        path = new Path(size, move);
      }
    }

    if (path.move != -1) {
      int[] movePos = new int[] { start[0] + directions[path.move][0], start[1] + directions[path.move][1] };

      if (this.board.isUnavailable(movePos, playArea)) {
        System.err.println("error invalid move");

      } else if (this.board.isUnavailable(movePos, this.board.getUnInflated())) {
        System.err.println("why isUnavailable for getUnInflated not playArea");
      }
    }

    return path;
  }

  private int isClosestToApple() {
    int[] goal = this.board.getApplePos();
    int[] myHeadPos = this.board.getMyHead();

    Path myPath = bfs(myHeadPos, goal, this.board.getPossible());

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
      Path enemyPath = bfs(headPos, goal, this.board.getUnInflated());// has to be uninflated

      if (enemyPath.size > -1 && myPath.size > enemyPath.size) {
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

    boolean trapped = bfs(pos, freestCorner, playArea).move == -1;
    return trapped;
  }

  private int[] generateApple(boolean[][] playArea) {

    int x = -1;
    int y = -1;
    int[] apple = { x, y };

    final int maxIterations = 200;
    int iterations = 0;
    boolean found = false;

    while (!found) {

      iterations++;
      if (iterations >= maxIterations) {
        break;
      }

      x = (int) (Math.random() * this.board.getWidth());
      y = (int) (Math.random() * this.board.getHeight());
      apple = new int[] { x, y };

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
    Path fakePath = bfs(myHead, fakeApplePos, playArea);

    if (fakePath.move == -1 || fakeApplePos[0] == -1 || Arrays.equals(board.getMyHead(), fakeApplePos)
        || trapped(fakeApplePos, playArea) || this.board.isCloseHead(fakeApplePos)) {
      found = false;
    }

    final int maxIterations = 50;
    int iterations = 0;

    while (!found) {
      iterations++;

      if (iterations >= maxIterations) {
        return -1;
      }

      fakeApplePos = generateApple(playArea);

      if (this.board.outOfBounds(fakeApplePos, 0)) {
        continue;
      }

      if (trapped(fakeApplePos, playArea)) {
        continue;
      }

      fakePath = bfs(myHead, fakeApplePos, playArea);

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

    boolean[][] lowerZombie = this.board.inflateAllHeads(3, 1);
    boolean[][] lowerEnemy = this.board.inflateAllHeads(1, 3);
    boolean[][] lowInflation = this.board.inflateAllHeads(1, 1);
    int move = -1;

    move = gaslight(this.board.getPossible());// has to be same as apple
    if (move != -1) {
      System.err.println("gaslight possible");
      return move;
    }

    move = gaslight(lowerZombie);
    if (move != -1) {
      System.err.println("gaslight lower zom");
      return move;
    }

    move = gaslight(lowerEnemy);
    if (move != -1) {
      System.err.println("gaslight lower enem");
      return move;
    }

    move = gaslight(lowInflation);
    if (move != -1) {
      System.err.println("gaslight lowest");
      return move;
    }

    move = lastResort(lowInflation);
    if (move != -1) {
      System.err.println("lastResort low");
      return move;
    }

    move = lastResort(this.board.inflateAllHeads(0, 1));
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