import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class KMeans extends Configured implements Tool {

  public static String INPUT_DIR = "/kmeans/input";
  public static String OUTPUT_DIR = "/kmeans/output";
  public static String CENTROIDS_DIR = "/kmeans/centroids";
  
  public static class KmMapper extends 
      Mapper<LongWritable, Text, LongWritable, Text> {

    private int K;
    private double[] centroidsX;
    private double[] centroidsY;
    private double[] centroidsZ;

    @Override
    protected void setup(Context context
        ) throws IOException, InterruptedException {
      K = Integer.valueOf(context.getConfiguration().get("kmeans.k"));
      try {
        Path centroidsFilePath = new Path(CENTROIDS_DIR);
        FileSystem fs = FileSystem.get(context.getConfiguration());
        BufferedReader cacheReader = new BufferedReader(
            new InputStreamReader(fs.open(centroidsFilePath)));
        centroidsX = new double[K];
        centroidsY = new double[K];
        centroidsZ = new double[K];
        int count = 0;
        String line;

        while ((count < K) && ((line = cacheReader.readLine()) != null)) {
          String[] xyz = line.split(" ");
          centroidsX[count] = Double.valueOf(xyz[0]);
          centroidsY[count] = Double.valueOf(xyz[1]);
          centroidsZ[count] = Double.valueOf(xyz[2]);
          count++;
        }
        cacheReader.close();
      } finally {
      }
    }

    @Override
    public void run(Context context
	) throws IOException, InterruptedException {
      setup(context);
      try {
        final int _K = K;
        final double maxDistance = Double.MAX_VALUE;
        final double[] pointsX;
        final double[] pointsY;
        final double[] pointsZ;
        List<Double> pointsListX = new ArrayList<Double>();
        List<Double> pointsListY = new ArrayList<Double>();
        List<Double> pointsListZ = new ArrayList<Double>();
        final int[] nearestCentroids;

	while(context.nextKeyValue()) {
	  String[] xyz = context.getCurrentValue().toString().split(" ");
          pointsListX.add(Double.valueOf(xyz[0]));
          pointsListY.add(Double.valueOf(xyz[1]));
          pointsListZ.add(Double.valueOf(xyz[2]));
	}

        final int size = pointsListX.size();
        pointsX = new double[size];
        pointsY = new double[size];
        pointsZ = new double[size];
        nearestCentroids = new int[size];
        for(int index = 0;index < size;index++) {
          pointsX[index] = pointsListX.get(index);
          pointsY[index] = pointsListY.get(index);
          pointsZ[index] = pointsListZ.get(index);
        }

        for(int index = 0;index < size;index++) {
          double minDistance = maxDistance;
          int nearestCentroid = -1;
          for(int i = 0;i < _K;i++) {
            double diffX = pointsX[i] - centroidsX[i];
            double diffY = pointsY[i] - centroidsY[i];
            double diffZ = pointsZ[i] - centroidsZ[i];
            double distance = diffX * diffX + diffY * diffY + diffZ * diffZ;
            if(distance < minDistance) {
              minDistance = distance;
              nearestCentroid = i;
            }
          }
          nearestCentroids[index] = nearestCentroid;
        }

        for(int index = 0;index < size;index++) {
          String point = pointsX[index] + " "
              + pointsY[index] + " " + pointsZ[index];
          context.write(new LongWritable(nearestCentroids[index]), 
              new Text(point));
        }
      } finally {
	cleanup(context);
      }
    }

    int getTaskID(Context context)
		throws IOException, InterruptedException {
      String attempID = context.getTaskAttemptID().getTaskID().toString();
      String[] parts = attempID.split("_");
      int taskID = Integer.valueOf(parts[4]);

      return taskID;
    }
  }

  public static class KmReducer extends 
      Reducer<LongWritable, Text, NullWritable, Text> {
    
    public void reduce(LongWritable oldCentroid,
        Iterable<Text> points, Context context)
        throws IOException, InterruptedException {
      double newCentroidX = 0.0;
      double newCentroidY = 0.0;
      double newCentroidZ = 0.0;
      int numElements = 0;
      List<Text> pointsList = new ArrayList<Text>();

      for(Text point : points) {
        pointsList.add(new Text(point.toString()));
        String[] xyz = point.toString().split(" ");
        newCentroidX += Double.valueOf(xyz[0]);
        newCentroidY += Double.valueOf(xyz[1]);
        newCentroidZ += Double.valueOf(xyz[2]);
        numElements++;
      }
      newCentroidX /= numElements;
      newCentroidY /= numElements;
      newCentroidZ /= numElements;
      
      context.write(NullWritable.get(), 
          new Text(newCentroidX + " " + newCentroidY + " " + newCentroidZ));
      for(Text point : pointsList) {
        context.write(NullWritable.get(), point); 
      }
    }
  }

  public static void kmeans(int K, Configuration conf
      ) throws IOException, ClassNotFoundException, InterruptedException {
    Job job = new Job(conf);
    //setup job conf
    job.setJobName(KMeans.class.getSimpleName());
    job.setJarByClass(KMeans.class);

    job.setInputFormatClass(TextInputFormat.class);

    job.setMapOutputKeyClass(LongWritable.class);
    job.setMapOutputValueClass(Text.class);
    job.setOutputKeyClass(NullWritable.class);
    job.setOutputValueClass(Text.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    job.setMapperClass(KmMapper.class);

    job.setReducerClass(KmReducer.class);
    job.setNumReduceTasks(K);

    job.setSpeculativeExecution(false);

    final Path inDir = new Path(INPUT_DIR);
    final Path outDir = new Path(OUTPUT_DIR);
    FileInputFormat.setInputPaths(job, inDir);
    FileOutputFormat.setOutputPath(job, outDir);

    final FileSystem fs = FileSystem.get(conf);

    try {
      System.out.println("Starting Job");
      final long startTime = System.currentTimeMillis();
      job.waitForCompletion(true);
      final double duration = (System.currentTimeMillis() - startTime)/1000.0;
      System.out.println("Job Finished in " + duration + " seconds");
    } finally {
      fs.delete(outDir, true);
    }
  }

  public int run(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("Usage: " + getClass().getName() + " <K>");
      ToolRunner.printGenericCommandUsage(System.err);
      return 2;
    }
    
    final int K = Integer.parseInt(args[0]);
    Configuration conf = getConf();
    conf.setInt("kmeans.k", K);

    System.out.println("Value of K = " + K);
        
    kmeans(K, getConf());
    return 0;
  }

  public static void main(String[] argv) throws Exception {
    System.exit(ToolRunner.run(null, new KMeans(), argv));
  }
}
