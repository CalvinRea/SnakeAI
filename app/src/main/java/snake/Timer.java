
package snake;

public class Timer {
  private long startTime;
  private long elapsedTime;
  private boolean running;

  public Timer() {
    reset();
  }

  public void start() {
    this.startTime = System.nanoTime();
    this.running = true;
  }

  public void stop() {
    if (running) {
      this.elapsedTime = System.nanoTime() - startTime;
      this.running = false;
    }
  }

  public long getElapsedTime() {
    if (running) {
      return System.nanoTime() - startTime;
    } else {
      return elapsedTime;
    }
  }

  public long getElapsedTimeMillis() {
    return getElapsedTime() / 1_000_000;
  }

  
  public void reset() {
    this.startTime = 0;
    this.elapsedTime = 0;
    this.running = false;
  }

  public boolean isRunning() {
    return running;
  }
}
