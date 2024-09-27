package snake;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
  private static BufferedWriter writer;

  static {
    try {
      writer = new BufferedWriter(new FileWriter("/home/cal/Videos/SnakeAI/app/logs/log.txt", true));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void log(String line) {
    try {
      writer.write(line);
      writer.newLine();
      writer.flush(); 
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
