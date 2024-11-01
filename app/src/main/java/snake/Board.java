package snake;

import java.util.*;

public class Board {

  private int width;

  private int height;

  private boolean[][] possible;

  private boolean[][] unInflated;

  private int length;

  private int[] applePos;

  private int[] myHead;

  private int[][] zombieHeads;

  private ArrayList<int[]> enemyHeads;

  private ArrayList<Integer> enemyLengths;

  private final int headDist = 6;

  private final int enemyInflation = 1;

  private final int zombieInflation = 1;

  public Board(HashMap<String, ArrayList<String>> lines, int inWidth,
      int inHeight, int myHeadNum) {
    this.width = inWidth;
    this.height = inHeight;

    String arr[] = lines.get("appleLine").get(0).split(" ");
    this.applePos = new int[] { Integer.parseInt(arr[0]), Integer.parseInt(arr[1]) };
    this.zombieHeads = new int[lines.get("zombieLines").size()][2];
    this.enemyHeads = new ArrayList<>();
    this.enemyLengths = new ArrayList<>();
    this.possible = new boolean[this.height][this.width];

    for (int i = 0; i < this.possible.length; i++) {
      for (int j = 0; j < (this.possible[i]).length; j++)
        this.possible[i][j] = true;
    }

    drawObstacles(lines.get("obstacleLines"));
    drawZombies(lines.get("zombieLines"));
    drawSnakes(lines.get("snakeLines"), myHeadNum);

    this.unInflated = copyOf(this.possible);
    this.possible = inflateAllHeads(enemyInflation, zombieInflation);
  }

  private void drawObstacles(ArrayList<String> obstacleLines) {

    for (String line : obstacleLines) {
      String[] obs = line.split(" ");

      for (String string : obs) {
        String[] pos = string.split(",");
        this.possible[Integer.parseInt(pos[1])][Integer.parseInt(pos[0])] = false;
      }
    }
  }

  private void drawZombies(ArrayList<String> zombieLines) {
    for (int i = 0; i < zombieLines.size(); i++) {
      String[] zomParts = zombieLines.get(i).split(" ");
      String[] head = zomParts[0].split(",");
      int[] headPos = { Integer.parseInt(head[0]), Integer.parseInt(head[1]) };
      this.zombieHeads[i] = headPos;
      for (int j = 0; j < zomParts.length - 1; j++) {
        drawLine(zomParts[j], zomParts[j + 1]);
      }
    }
  }

  private void drawSnakes(ArrayList<String> snakeLines, int myHeadNum) {
    for (int i = 0; i < snakeLines.size(); i++) {
      String[] snakeParts = snakeLines.get(i).split(" ");
      if (i == myHeadNum) {
        this.length = Integer.parseInt(snakeParts[1]);
        String[] head = snakeParts[3].split(",");
        this.myHead = new int[] { Integer.parseInt(head[0]), Integer.parseInt(head[1]) };
        for (int j = 3; j < snakeParts.length - 1; j++) {
          drawLine(snakeParts[j], snakeParts[j + 1]);
        }

      } else {
        String[] head = snakeParts[3].split(",");
        int[] headPos = { Integer.parseInt(head[0]), Integer.parseInt(head[1]) };
        this.enemyHeads.add(headPos);

        int length = Integer.parseInt(snakeParts[1]);
        this.enemyLengths.add(length);

        for (int j = 3; j < snakeParts.length - 1; j++) {
          drawLine(snakeParts[j], snakeParts[j + 1]);
        }
      }
    }
  }

  private void drawLine(String a, String b) {
    int aX = Integer.parseInt(a.split(",")[0]);
    int bX = Integer.parseInt(b.split(",")[0]);
    int aY = Integer.parseInt(a.split(",")[1]);
    int bY = Integer.parseInt(b.split(",")[1]);
    int minX = Math.min(aX, bX);
    int maxX = Math.max(aX, bX);
    int minY = Math.min(aY, bY);
    int maxY = Math.max(aY, bY);
    for (int i = minX; i <= maxX; i++) {
      this.possible[minY][i] = false;
    }

    for (int j = minY; j <= maxY; j++) {
      this.possible[j][minX] = false;
    }

  }

  public boolean[][] inflateAllHeads(int enemyInflation, int zombieInflation) {
    boolean[][] copy = copyOf(this.unInflated);

    for (int i = 0; i < this.enemyHeads.size(); i++) {
      int[] headPos = this.enemyHeads.get(i);
      inflateHead(headPos, enemyInflation, copy);
    }
    for (int i = 0; i < this.zombieHeads.length; i++) {
      int[] headPos = this.zombieHeads[i];
      inflateHead(headPos, zombieInflation, copy);
    }
    return copy;
  }

  public boolean isCloseHead(int[] pos) {
    for (int[] zombieHead : zombieHeads) {
      if (manDist(pos, zombieHead) < headDist) {
        return true;
      }
    }

    for (int[] enemyHead : enemyHeads) {
      if (manDist(pos, enemyHead) < headDist) {
        return true;
      }
    }

    return false;
  }

  private void inflateHead(int[] position, int maxLevel, boolean[][] board) {

    int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
    boolean[][] visited = new boolean[this.height][this.width];
    Queue<int[]> queue = new LinkedList<>();
    queue.add(new int[] { position[0], position[1], 0 });
    visited[position[1]][position[0]] = true;

    while (!queue.isEmpty()) {
      int[] currentPos = queue.poll();
      int currentLevel = currentPos[2];
      board[currentPos[1]][currentPos[0]] = false;

      if (currentLevel >= maxLevel) {
        continue;
      }

      for (int[] direction : directions) {
        int[] newPos = { currentPos[0] + direction[0], currentPos[1] + direction[1] };

        if (!isUnavailable(newPos, board) && !visited[newPos[1]][newPos[0]]) {
          visited[newPos[1]][newPos[0]] = true;
          queue.add(new int[] { newPos[0], newPos[1], currentLevel + 1 });
        }
      }
    }
  }

  public boolean isUnavailable(int[] position, boolean[][] playArea) {
    return position[0] < 0 || position[0] > this.width - 1 || position[1] < 0 || position[1] > this.height - 1
        || !playArea[position[1]][position[0]];
  }

  public boolean outOfBounds(int[] position, int gap) {
    return (position[0] < gap || position[0] > this.width - 1 - gap || position[1] < gap
        || position[1] > this.height - 1 - gap);
  }

  public boolean[][] copyOf(boolean[][] original) {
    int width = original[0].length;
    int height = original.length;
    boolean[][] copy = new boolean[height][width];

    for (int i = 0; i < height; i++) {
      System.arraycopy(original[i], 0, copy[i], 0, width);
    }
    return copy;
  }

  private int manDist(int[] pos1, int[] pos2) {
    return Math.abs(pos1[0] - pos2[0]) + Math.abs(pos1[1] - pos2[1]);
  }

  public int getLength() {
    return this.length;
  }

  public boolean[][] getUnInflated() {
    return this.unInflated;
  }

  public int[] getApplePos() {
    return this.applePos;
  }

  public ArrayList<int[]> getEnemyHeads() {
    return this.enemyHeads;
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  public int[] getMyHead() {
    return this.myHead;
  }

  public int[][] getZombieHeads() {
    return zombieHeads;
  }

  public int getEnemyInflation() {
    return enemyInflation;
  }

  public boolean[][] getPossible() {
    return possible;
  }

  public ArrayList<Integer> getEnemyLengths() {
    return enemyLengths;
  }
}