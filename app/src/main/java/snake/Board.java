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

  public Board(HashMap<String, ArrayList<String>> lines, int inWidth,
      int inHeight, int myHeadNum) {

    width = inWidth;
    height = inHeight;

    String arr[] = lines.get("appleLine").get(0).split(" ");
    applePos = new int[] { Integer.parseInt(arr[0]), Integer.parseInt(arr[1]) };

    zombieHeads = new int[lines.get("zombieLines").size()][2];
    enemyHeads = new ArrayList<>();

    possible = new boolean[height][width];
    for (int i = 0; i < possible.length; i++) {
      for (int j = 0; j < possible[i].length; j++) {
        possible[i][j] = true;
      }
    }

    drawObstacles(lines.get("obstacleLines"));
    drawZombies(lines.get("zombieLines"));
    drawSnakes(lines.get("snakeLines"), myHeadNum);
    length = calculateLength(lines.get("snakeLines").get(myHeadNum));
    unInflated = copyOf(possible);

    possible = inflateAllHeads(1, 1);// changed from 2,2
  }

  private void drawObstacles(ArrayList<String> obstacleLines) {

    for (String line : obstacleLines) {
      String[] obs = line.split(" ");
      for (String string : obs) {
        String[] pos = string.split(",");
        possible[Integer.parseInt(pos[1])][Integer.parseInt(pos[0])] = false;
      }
    }
  }

  private void drawZombies(ArrayList<String> zombieLines) {

    for (int i = 0; i < zombieLines.size(); i++) {
      String[] zomParts = zombieLines.get(i).split(" ");
      String[] head = zomParts[0].split(",");
      int[] headPos = { Integer.parseInt(head[0]), Integer.parseInt(head[1]) };
      zombieHeads[i] = headPos;

      for (int j = 0; j < zomParts.length - 1; j++) {
        drawLine(zomParts[j], zomParts[j + 1]);
      }
    }
  }

  private void drawSnakes(ArrayList<String> snakeLines, int myHeadNum) {

    for (int i = 0; i < snakeLines.size(); i++) {
      String[] snakeParts = snakeLines.get(i).split(" ");
      if (i == myHeadNum) {
        String[] head = snakeParts[3].split(",");
        myHead = new int[] { Integer.parseInt(head[0]), Integer.parseInt(head[1]) };
        for (int j = 3; j < snakeParts.length - 1; j++) {
          drawLine(snakeParts[j], snakeParts[j + 1]);
        }

      } else {
        String[] head = snakeParts[3].split(",");
        int[] headPos = { Integer.parseInt(head[0]), Integer.parseInt(head[1]) };
        enemyHeads.add(headPos);
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
      possible[minY][i] = false;
    }

    for (int j = minY; j <= maxY; j++) {
      possible[j][minX] = false;
    }
  }

  public boolean[][] inflateAllHeads(int enemyInflation, int zombieInflation) {
    if (enemyInflation > 1 || zombieInflation > 1) {
      Exception e = new Exception("Don't be an idiot");
      try {
        throw e;
      } catch (Exception f) {
        f.printStackTrace();
      }
    }

    boolean[][] copy = copyOf(unInflated);
    if (enemyInflation == 1) {
      for (int i = 0; i < enemyHeads.size(); i++) {
        int[] headPos = enemyHeads.get(i);
        inflateHead(headPos, copy);
      }
    }

    if (zombieInflation == 1) {
      for (int i = 0; i < zombieHeads.length; i++) {
        int[] headPos = zombieHeads[i];
        inflateHead(headPos, copy);
      }
    }
    return copy;
  }

  private void inflateHead(int[] head, boolean[][] board) {

    int[][] directions = { { 0, 1 }, { 0, -1 }, { 1, 0 }, { -1, 0 } };
    for (int[] direction : directions) {
      int[] newPos = new int[] { direction[0] + head[0], direction[1] + head[1] };
      if (!outOfBounds(newPos)) {
        board[newPos[1]][newPos[0]] = false;
      }
    }
  }

  public boolean isUnavailable(int[] position, boolean[][] playArea) {
    return position[0] < 0 || position[0] >= width || position[1] < 0 ||
        position[1] >= height || !playArea[position[1]][position[0]];
  }

  public boolean outOfBounds(int[] position) {
    return position[0] < 0 || position[0] >= width || position[1] < 0 ||
        position[1] >= height;
  }

  private boolean[][] copyOf(boolean[][] original) {
    boolean[][] copy = new boolean[50][50];
    for (int i = 0; i < 50; i++) {
      for (int j = 0; j < 50; j++) {
        copy[i][j] = original[i][j];
      }
    }
    return copy;
  }

  public int calculateLength(String myString) {
    String[] snakeParts = myString.split(" ");
    return Integer.parseInt(snakeParts[1]);
  }

  public int getLength() {
    return length;
  }

  public boolean[][] getPossible() {
    return possible;
  }

  public boolean[][] getUnInflated() {
    return unInflated;
  }

  public int[] getApplePos() {
    return applePos;
  }

  public ArrayList<int[]> getEnemyHeads() {
    return enemyHeads;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public int[] getMyHead() {
    return myHead;
  }

}
