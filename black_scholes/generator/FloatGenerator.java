import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

public class FloatGenerator {

  private static final String OUTPUT_PREFIX =
      "/data/mytmp/black_scholes/input/";
  private static final int MAX_NUM_PARTS = 4000;
  private static final int MAX_NUM_FLOATS_PER_PART = (int)(1024 * 1024 * 5);
  private static final Random RANDOM = new Random();

  public static void main(String[] args) {
    for(int part = 100;part < MAX_NUM_PARTS;part++) {
      generate(OUTPUT_PREFIX + "part" + part);
    }
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
