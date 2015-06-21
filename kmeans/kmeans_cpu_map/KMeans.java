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

  public static String INPUT_DIR     = "/user/yiwei/kmeans/input";
  public static String OUTPUT_DIR    = "/user/yiwei/kmeans/output";
  public static String CENTROIDS_DIR = "/user/yiwei/kmeans/centroids";
  
  public static class KmMapper extends 
      Mapper<LongWritable, Text, LongWritable, Text> {

    private int K;
    private final double maxDistance = Double.MAX_VALUE;
    private double[] centroidsX;
    private double[] centroidsY;
    private double[] centroidsZ;
    private long mapForStartTime = 0l;
    private long mapForDuration  = 0l;
    private long prepareStartTime = 0l;
    private long prepareDuration  = 0l;
    private long makeStringStartTime   = 0l;
    private long makeStringDuration    = 0l;

    @Override
    protected void setup(Context context
        ) throws IOException, InterruptedException {
      K = Integer.valueOf(context.getConfiguration().get("kmeans.k"));
      try {
        Path centroidsFilePath = new Path(CENTROIDS_DIR, "part0");
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
    protected void cleanup(Context Context
            ) throws IOException, InterruptedException{
        System.out.println("map prepare duration\t"+prepareDuration);
        System.out.println("map for duration\t"+mapForDuration);
        System.out.println("map makeString duration\t"+makeStringDuration);
    }

    @Override
    protected void map(LongWritable key, Text value, Context context
            ) throws IOException, InterruptedException {
                prepareStartTime = System.currentTimeMillis();
                String[] xyz = value.toString().split(" ");
                Double pointX = Double.valueOf(xyz[0]);
                Double pointY = Double.valueOf(xyz[1]);
                Double pointZ = Double.valueOf(xyz[2]);
                prepareDuration += System.currentTimeMillis() -prepareStartTime;

                mapForStartTime = System.currentTimeMillis();
                double minDistance = maxDistance;
                int nearestCentroid = -1;
                for(int i = 0;i < K;i++) {
                    double diffX = pointX - centroidsX[i];
                    double diffY = pointY - centroidsY[i];
                    double diffZ = pointZ - centroidsZ[i];
                    double distance = diffX * diffX + diffY * diffY + diffZ * diffZ;
                    if(distance < minDistance) {
                      minDistance = distance;
                      nearestCentroid = i;
                    }
                }
                mapForDuration += System.currentTimeMillis() - mapForStartTime;

                makeStringStartTime = System.currentTimeMillis();
                String point = pointX + " "
                    + pointY + " " + pointZ;
                makeStringDuration += System.currentTimeMillis() - makeStringStartTime;
                context.write(new LongWritable(nearestCentroid), 
                  new Text(point));
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
    // Job job = new Job(conf) is deprecated
    Job job = Job.getInstance(conf);
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
    job.setNumReduceTasks(1);

    job.setSpeculativeExecution(false);

    long timestamp = System.currentTimeMillis();
    final Path inDir = new Path(INPUT_DIR);
    final Path outDir = new Path(OUTPUT_DIR+timestamp);
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
    // for profiling using HPROF
    conf.setBoolean("mapreduce.task.profile", true);
//    conf.set("mapreduce.task.profile.params","-agentlib:hprof=cpu=samples,depth=40,force=n,thread=y,verbose=n,file=%s");
    // using BTrace
    conf.set("mapreduce.task.profile.params", "-javaagent:"
            + "/home/yiwei/btrace/map/btrace-agent.jar="
            + "dumpClasses=false,debug=false,"
            + "unsafe=true,probeDescPath=.,noServer=true,"
            + "script=/home/yiwei/btrace/map/KMeansMap.class,"
            + "scriptOutputFile=%s");
    conf.set("mapreduce.task.profile.maps","0");
    conf.set("mapreduce.task.profile.reduces","0");
    conf.setInt("mapreduce.job.jvm.numtasks", 1);
    conf.setInt("mapreduce.map.combine.minspills", 9999);
    conf.setBoolean("mapreduce.map.speculative", false);
    conf.setBoolean("mapreduce.reduce.speculative", false);
    conf.setInt("mapreduce.reduce.shuffle.parallelcopies", 1);

    conf.setFloat("mapreduce.reduce.input.buffer.percent", 1.0f);
    conf.setInt("mapreduce.reduce.merge.inmem.threshold ", 0);
    conf.setInt("mapreduce.task.io.sort.mb", 300);
//    conf.setBoolean("mapreduce.map.output.compress", true);

    System.out.println("Value of K = " + K);
        
    kmeans(K, getConf());
    return 0;
  }

  public static void main(String[] argv) throws Exception {
    System.exit(ToolRunner.run(null, new KMeans(), argv));
  }
}
