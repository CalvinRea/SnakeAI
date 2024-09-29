package snake;

import java.io.*;
import java.util.*;
import za.ac.wits.snake.DevelopmentAgent;

//TODO: change back to A* if necessary
public class Snek extends DevelopmentAgent {

  private final int[][] directions = new int[][] { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 } };
  private final int numObstacles = 3;
  private final int numZombies = 3;

  private Board board;
  private int[] fakeApplePos = { -1, -1 };

  public static void main(String[] args) {
    Snek agent = new Snek();
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
    int move = -1;
    move = isClosestToApple();
    if (move != -1) {
      System.err.println("Apple move");
      return move;
    }

    move = survivalMove();
    if (move != -1) {
      return move;
    }

    System.err.println("No move found");
    move = 0;
    return move;
  }

  private Path bfs(int[] start, int[] goal, boolean[][] playArea) {

    Path path = new Path(-1, -1);

    if (Arrays.equals(start, goal)) {
      System.err.println("already at goal");
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
          }
        }

        visited.add(neighborHash);
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

  private int hashPosition(int[] position) {
    return position[1] * board.getWidth() + position[0];
  }

  private int[] unhashPosition(int hash) {
    int x = hash % board.getWidth();
    int y = hash / board.getWidth();
    return new int[] { x, y };
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

  private int[] generateApple(boolean[][] playArea) {
    int offSet = 10;
    int x = (int) Math.round(Math.random() * (this.board.getWidth() - offSet * 2) + offSet);
    int y = (int) Math.round(Math.random() * (this.board.getHeight() - offSet * 2) + offSet);
    int[] apple = new int[] { x, y };

    while (this.board.isUnavailable(apple, playArea) || Arrays.equals(board.getMyHead(), apple)) {
      x = (int) Math.round(Math.random() * (this.board.getWidth() - offSet * 2) + offSet);
      y = (int) Math.round(Math.random() * (this.board.getHeight() - offSet * 2) + offSet);
      apple = new int[] { x, y };
    }
    return apple;
  }

  private boolean trapped(boolean[][] playArea) {
    boolean trapped = bfs(this.board.getMyHead(), new int[] { 0, 0 }, playArea).move == -1;
    return trapped;
  }

  private int fakeMove(boolean[][] playArea) {

    if (trapped(playArea)) {
      return -1;
    }

    if (this.board.isUnavailable(fakeApplePos, playArea)
        || fakeApplePos[0] == -1 || Arrays.equals(board.getMyHead(), fakeApplePos)) {
      fakeApplePos = generateApple(playArea);
    }

    int[] myHeadPos = this.board.getMyHead();
    Path fakePath = bfs(myHeadPos, fakeApplePos, playArea);

    final int maxIterations = 50;
    int iterations = 0;

    while (fakePath.move == -1 && iterations < maxIterations) {
      fakeApplePos = generateApple(playArea);
      fakePath = bfs(myHeadPos, fakeApplePos, playArea);
      iterations++;
    }

    if (iterations == maxIterations) {
      return -1;
    }

    int[] movePos = new int[] {
        this.board.getMyHead()[0] + directions[fakePath.move][0],
        this.board.getMyHead()[1] + directions[fakePath.move][1] };

    if (this.board.isUnavailable(movePos, playArea)) {
      System.err.println("error invalid move");
    }

    return fakePath.move;
  }

  private int lastResort(boolean[][] playArea) {
    int[] myHead = this.board.getMyHead();

    for (int i = 0; i < directions.length; i++) {
      int[] newPos = { myHead[0] + directions[i][0], myHead[1] + directions[i][1] };

      if (!this.board.isUnavailable(newPos, playArea)) {
        return i;
      }
    }
    return -1;
  }

  private int survivalMove() {

    boolean[][] lowInflation = this.board.inflateAllHeads(1, 1);
    int move = -1;
    if (!trapped(lowInflation)) {
      move = fakeMove(this.board.getPossible());// has to be same as apple
      if (move != -1) {
        System.err.println("Slow Fake Apple");
        return move;
      }

      move = fakeMove(lowInflation);
      if (move != -1) {
        System.err.println("Fast Fake Apple");
        return move;
      }
    }
    move = lastResort(lowInflation);
    if (move != -1) {
      System.err.println("Last Resort");
      return move;
    }

    move = lastResort(this.board.inflateAllHeads(0, 0));
    System.err.println("BANZAI");
    return move;
  }

  /* UTILITY METHODS */

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
}