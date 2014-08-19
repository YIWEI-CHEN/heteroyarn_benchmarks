import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BooleanWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.NullOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class GaussianBlur extends Configured implements Tool {
  
  public static class GBMapper extends 
      Mapper<LongWritable, Text, Text, Text> {

    @Override
    public void run(Context context)
		    throws IOException, InterruptedException {
      setup(context);
      int iteration = 0;
      try {
        final double sigma = Double.valueOf(context.getConfiguration().get("gaussian_blur.sigma"));
	while(context.nextKeyValue()) {
	  String _image = context.getCurrentValue().toString();
	  String[] image = context.getCurrentValue().toString().split(" ");
          final int height = Integer.valueOf(image[0]);
          final int width = Integer.valueOf(image[1]);
          final int[][] red = new int[height][width];
          final int[][] green = new int[height][width];
          final int[][] blue = new int[height][width];
          final int[][] rgbDest = new int[height][width];


          int index = 2;
          for(int x = 0;x < height;x++) {
            for(int y = 0;y < width;y++) {
              red[x][y] = Integer.valueOf(image[index]);
              green[x][y] = Integer.valueOf(image[index + 1]);
              blue[x][y] = Integer.valueOf(image[index + 2]);
              index += 3;
            }
          }

          final double[][] mask = createBlurMask(sigma);
          final int maskSize = mask.length / 2;

          for(int x = 0;x < height;x++) {
            for(int y = 0;y < width;y++) {
              double sumRed = 0.0, sumGreen = 0.0, sumBlue = 0.0;
              for(int a = -maskSize; a < maskSize + 1; a++) {
                for(int b = -maskSize; b < maskSize + 1; b++) {
                  int readX = a + x, readY = b + y;
                  boolean outOfBound = (readX < 0) || (readX >= height) 
                      || (readY < 0) || (readY >= width);
                  if(!outOfBound) {
                    sumRed += mask[a + maskSize][b + maskSize] * red[readX][readY];
                    sumGreen += mask[a + maskSize][b + maskSize] * green[readX][readY];
                    sumBlue += mask[a + maskSize][b + maskSize] * blue[readX][readY];
                  }
                }
              }
              rgbDest[x][y] = ((int)sumBlue) | ((int)sumGreen << 8) | ((int)sumRed << 16);
            }
          }

          BufferedImage imageDest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
          for(int x = 0;x < height;x++) {
            for(int y = 0;y < width;y++) {
              imageDest.setRGB(y, x, rgbDest[x][y]);
            }
          }
          ImageIO.write(imageDest, "jpg", new File("/home/heterohadoop/gaussian_blur_output/gb_output_" 
              + iteration + "_" + getTaskID(context) + ".jpg"));
          iteration++;
	}

      } finally {
	cleanup(context);
      }
    }

    public double[][] createBlurMask(double sigma) {
      int maskSize = (int)Math.ceil(3.0 * sigma);
      double[] _mask = new double[(maskSize * 2 + 1) * (maskSize * 2 + 1)];
      double sum = 0.0f;
      for(int a = -maskSize; a < maskSize + 1; a++) {
        for(int b = -maskSize; b < maskSize + 1; b++) {
          double temp = Math.exp(-((double)(a * a + b * b) / (2 * sigma * sigma)));
          sum += temp;
          _mask[a + maskSize + (b + maskSize) * (maskSize * 2 + 1)] = temp;
        }
      }
      for(int i = 0; i < (maskSize * 2 + 1 ) * (maskSize * 2 + 1); i++) {
          _mask[i] = _mask[i] / sum;
      }

      double[][] mask = new double[maskSize * 2 + 1][maskSize * 2 + 1];
      int index = 0;
      for(int i = 0;i < (maskSize * 2 + 1);i++) {
        for(int j = 0;j < (maskSize * 2 + 1);j++) {
          mask[i][j] = _mask[index];
          index++;
        }
      }

      return mask;
    }

    int getTaskID(Context context)
		throws IOException, InterruptedException {
      String attempID = context.getTaskAttemptID().getTaskID().toString();
      String[] parts = attempID.split("_");
      int taskID = Integer.valueOf(parts[4]);

      return taskID;
    }
  }

  public static void executeGaussianBlur(Configuration conf
      ) throws IOException, ClassNotFoundException, InterruptedException {
    Job job = new Job(conf);
    //setup job conf
    job.setJobName(GaussianBlur.class.getSimpleName());
    job.setJarByClass(GaussianBlur.class);

    job.setInputFormatClass(TextInputFormat.class);

    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(Text.class);
    job.setOutputFormatClass(NullOutputFormat.class);

    job.setMapperClass(GBMapper.class);

    job.setNumReduceTasks(0);

    // turn off speculative execution, because DFS doesn't handle
    // multiple writers to the same file.
    job.setSpeculativeExecution(false);

    //setup input/output directories
    final Path inDir = new Path("/images");
    FileInputFormat.setInputPaths(job, inDir);

    final FileSystem fs = FileSystem.get(conf);

    try {
      //start a job
      System.out.println("Starting Job");
      final long startTime = System.currentTimeMillis();
      job.waitForCompletion(true);
      final double duration = (System.currentTimeMillis() - startTime)/1000.0;
      System.out.println("Job Finished in " + duration + " seconds");

    } finally {
      //fs.delete(tmpDir, true);
    }
  }

  public int run(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: " + getClass().getName() + " <sigma>");
      ToolRunner.printGenericCommandUsage(System.err);
      return 2;
    }
    
    double sigma = Double.parseDouble(args[0]);
    Configuration conf = getConf();
    
    System.out.println("Value of sigma  = " + sigma);
    conf.setDouble("gaussian_blur.sigma", sigma);

    executeGaussianBlur(getConf());
    System.out.println("Gaussian Blur is done for sigma = " + sigma);

    return 0;
  }

  public static void main(String[] argv) throws Exception {
    System.exit(ToolRunner.run(null, new GaussianBlur(), argv));
  }
}
