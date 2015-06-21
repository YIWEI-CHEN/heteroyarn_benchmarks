import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

public class FloatGenerator {

  private static final String OUTPUT_PREFIX =
      "/mnt/data/project/hadoop-input/black_scholes/";
  private static final int MAX_NUM_PARTS = 1;
  private static final int MAX_NUM_FLOATS_PER_PART = (int)(1024 * 1024 * 3);
  private static final Random RANDOM = new Random();

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
      for(int i = 0;i < MAX_NUM_FLOATS_PER_PART;i++) {
        double value = randomDouble();
        writer.println(value);
      }
      writer.close();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  private static double randomDouble() {
    double val = RANDOM.nextDouble();
    val = (long)(val * 10E12) / (double)10E12;
    return val;
  }
}
