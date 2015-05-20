import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

public class PointGenerator {

  private static final String OUTPUT_PREFIX = "/mnt/data/project/hadoop-input/kmeans/";
  private static final int MAX_NUM_PARTS = 1;
  private static final int MAX_NUM_POINTS_PER_PART = (int)(1024 * 1024 * 10);
  private static final Random RANDOM = new Random();
  private static final double RANGE_MIN = 0.0;
  private static final double RANGE_MAX = 10000;

  public static void main(String[] args) {
    long start = System.currentTimeMillis();
    for(int part = 0;part < MAX_NUM_PARTS;part++) {
      generate(OUTPUT_PREFIX + "part" + part);
    }
    System.out.println((System.currentTimeMillis()-start)/1000f);
  }

  private static void generate(String outputName) {
    try {
      PrintWriter writer = new PrintWriter(new FileWriter(outputName, true));
      for(int i = 0;i < MAX_NUM_POINTS_PER_PART;i++) {
        double x = randomDouble();
        double y = randomDouble();
        double z = randomDouble();
        writer.println(x + " " + y + " " + z);
      }
      writer.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  private static double randomDouble() {
    double val = RANGE_MIN + (RANGE_MAX - RANGE_MIN) * RANDOM.nextDouble();
    val = (int)(val * 100) / 100.0;
    return val;
  }
}
