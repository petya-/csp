// Crude wall clock timing utility, measuring time in seconds
package org.simple.software;
import java.util.concurrent.TimeUnit;

public class Timer {
  private long start, spent = 0;
  public Timer() { play(); }
  public double check() { return (System.nanoTime()-start+spent)/1e9; } // returns time in seconds
  public void pause() { spent += System.nanoTime()-start; }
  public void play() { start = System.nanoTime(); }
  public void restart() { start = System.nanoTime(); }

}
