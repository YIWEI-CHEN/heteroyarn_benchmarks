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

import com.amd.aparapi.Kernel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class KMeans extends Configured implements Tool {
    enum AparapiCounters{
        APARAPI_CONVERSION_MILLIS,
        APARAPI_EXECUTION_MILLIS,
        APARAPI_BUFFER_WRITE_MILLIS,
        APARAPI_KERNEL_MILLIS,
        APARAPI_BUFFER_READ_MILLIS
    }

  public static String INPUT_DIR     = "kmeans/input";
  public static String OUTPUT_DIR    = "kmeans/output";
  public static String CENTROIDS_DIR = "kmeans/centroids";
  
  public static class KmMapper extends 
      Mapper<LongWritable, Text, LongWritable, Text> {

    //static final Log LOG = LogFactory.getLog(KmMapper.class);

    private int K;
    private double[] centroidsX;
    private double[] centroidsY;
    private double[] centroidsZ;
    private long copyDuration = 0l;
    private long prepareDuration  = 0l;
    private long addDuration = 0l;
    private long convertDuration = 0l;
    private long mapForDuration  = 0l;
    private long makeStringDuration    = 0l;
    private long aparapiConversionTime = 0L, aparapiExecutionTime = 0L,
          aparapiBufferWriteTime = 0L, aparapiKernelTime = 0L, aparapiBufferReadTime = 0L;

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
    protected void cleanup(Context context
            ) throws IOException, InterruptedException {
        System.out.println("copy array\t"+copyDuration);
        System.out.println("prepare duration\t"+prepareDuration);
        System.out.println("list add duration\t"+addDuration);
        System.out.println("convert to array duration\t"+convertDuration);
        System.out.println("for duration\t"+mapForDuration);
        System.out.println("makeString duration\t"+makeStringDuration);
        System.out.println("aparapiConversionTime \t"+aparapiConversionTime +"ms"); 
        System.out.println("aparapiExecutionTime  \t"+aparapiExecutionTime  +"ms"); 
        System.out.println("aparapiBufferWriteTime\t"+aparapiBufferWriteTime+"ms"); 
        System.out.println("aparapiKernelTime     \t"+aparapiKernelTime     +"ms"); 
        System.out.println("aparapiBufferReadTime \t"+aparapiBufferReadTime +"ms"); 
    }

    @Override
    public void run(Context context
	) throws IOException, InterruptedException {
      setup(context);

      try {
        final int _K = K;
        final double maxDistance = Double.MAX_VALUE;
        final double[] _centroidsX = new double[_K];
        final double[] _centroidsY = new double[_K];
        final double[] _centroidsZ = new double[_K];
        final double[] pointsX;
        final double[] pointsY;
        final double[] pointsZ;
        List<Double> pointsListX = new ArrayList<Double>();
        List<Double> pointsListY = new ArrayList<Double>();
        List<Double> pointsListZ = new ArrayList<Double>();
        final int[] nearestCentroids;
        // declaration for profiling
        long copyStartTime = 0l;
        long prepareStartTime = 0l;
        long addStartTime = 0l;
        long convertStartTime = 0l;
        long mapForStartTime = 0l;
        long makeStringStartTime = 0l;

        copyStartTime = System.currentTimeMillis();
        System.arraycopy(centroidsX, 0, _centroidsX, 0, _K);
        System.arraycopy(centroidsY, 0, _centroidsY, 0, _K);
        System.arraycopy(centroidsZ, 0, _centroidsZ, 0, _K);
        copyDuration = System.currentTimeMillis() - copyStartTime;
	while(context.nextKeyValue()) {
          Text value = context.getCurrentValue();
          prepareStartTime = System.currentTimeMillis();
          String[] xyz = value.toString().split(" ");
          Double x = Double.valueOf(xyz[0]);
          Double y = Double.valueOf(xyz[1]);
          Double z = Double.valueOf(xyz[2]);
          prepareDuration += System.currentTimeMillis() -prepareStartTime;
          addStartTime = System.currentTimeMillis();
          pointsListX.add(x);
          pointsListY.add(y);
          pointsListZ.add(z);
          addDuration += System.currentTimeMillis()-addStartTime;
	}

        final int size = pointsListX.size();
        pointsX = new double[size];
        pointsY = new double[size];
        pointsZ = new double[size];
        nearestCentroids = new int[size];
        convertStartTime = System.currentTimeMillis();
        for(int index = 0;index < size;index++) {
          pointsX[index] = pointsListX.get(index);
          pointsY[index] = pointsListY.get(index);
          pointsZ[index] = pointsListZ.get(index);
        }
        convertDuration = System.currentTimeMillis()-convertStartTime;

//        if(isGpuMapper()) {
        Kernel kernel = new Kernel() {
          @Override public void run() {
            int gid = getGlobalId();
            double minDistance = maxDistance;
            int nearestCentroid = -1;
            for(int i = 0;i < _K;i++) {
              double diffX = pointsX[gid] - _centroidsX[i];
              double diffY = pointsY[gid] - _centroidsY[i];
              double diffZ = pointsZ[gid] - _centroidsZ[i];
              double distance = diffX * diffX + diffY * diffY + diffZ * diffZ;
              if(distance < minDistance) {
                minDistance = distance;
                nearestCentroid = i;
              }
            }
            nearestCentroids[gid] = nearestCentroid;
          }
        };
        mapForStartTime = System.currentTimeMillis();
        kernel.execute(size);
        mapForDuration += System.currentTimeMillis() - mapForStartTime;
        System.out.println(kernel.getExecutionMode());
        aparapiConversionTime  += kernel.getConversionTime();
        aparapiExecutionTime   += kernel.getExecutionTime();
//        aparapiBufferWriteTime += kernel.getBufferHostToDeviceTime();
//        aparapiKernelTime      += kernel.getKernelExecutionTime();
//        aparapiBufferReadTime  += kernel.getBufferDeviceToHostTime();
        kernel.dispose();
        updateAparapiCounters(context, aparapiConversionTime, aparapiExecutionTime,
            aparapiBufferWriteTime, aparapiKernelTime, aparapiBufferReadTime);
//          System.out.println("taskID:"+getTaskID(context)+"execution time:"+aparapiExecutionTime);
//        } else {
//          for(int index = 0;index < size;index++) {
//            double minDistance = maxDistance;
//            int nearestCentroid = -1;
//            for(int i = 0;i < _K;i++) {
//              double diffX = pointsX[i] - centroidsX[i];
//              double diffY = pointsY[i] - centroidsY[i];
//              double diffZ = pointsZ[i] - centroidsZ[i];
//              double distance = diffX * diffX + diffY * diffY + diffZ * diffZ;
//              if(distance < minDistance) {
//                minDistance = distance;
//                nearestCentroid = i;
//              }
//            }
//            nearestCentroids[index] = nearestCentroid;
//          }
//        }

        for(int index = 0;index < size;index++) {
          makeStringStartTime = System.currentTimeMillis();
          String point = pointsX[index] + " "
              + pointsY[index] + " " + pointsZ[index];
          makeStringDuration += System.currentTimeMillis() - makeStringStartTime;
          System.out.println(index+"\t"+nearestCentroids[index]);
          context.write(new LongWritable(nearestCentroids[index]), 
              new Text(point));
        }
        
      } finally {
          //LOG.info("taskID: "+getTaskID(context)+" cpu time: "+context.getCounter(TaskCounter.CPU_MILLISECONDS).getValue());
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

    public void updateAparapiCounters(Context context,
				      long aparapiConversionTime, long aparapiExecutionTime,
				      long aparapiBufferWriteTime, long aparapiKernelTime, long aparapiBufferReadTime)
				      throws IOException, InterruptedException {
      context.getCounter(AparapiCounters.APARAPI_EXECUTION_MILLIS).increment(aparapiExecutionTime);
      context.getCounter(AparapiCounters.APARAPI_CONVERSION_MILLIS).increment(aparapiConversionTime);
      context.getCounter(AparapiCounters.APARAPI_EXECUTION_MILLIS).increment(aparapiExecutionTime);
      context.getCounter(AparapiCounters.APARAPI_BUFFER_WRITE_MILLIS).increment(aparapiBufferWriteTime);
      context.getCounter(AparapiCounters.APARAPI_KERNEL_MILLIS).increment(aparapiKernelTime);
      context.getCounter(AparapiCounters.APARAPI_BUFFER_READ_MILLIS).increment(aparapiBufferReadTime);
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
    // yiwei
  //  job.setNumMapTasks(8);
    job.setNumReduceTasks(1);

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
    // for profiling using HPROF
   // conf.setBoolean("mapreduce.task.profile", true);
////    conf.set("mapreduce.task.profile.params","-agentlib:hprof=cpu=samples,depth=6,force=n,thread=y,verbose=n,file=%s");
//    // using BTrace
    //conf.set("mapreduce.task.profile.params", "-javaagent:"
    //        + "/home/yiwei/btrace/gpu/btrace-agent.jar="
    //        + "dumpClasses=false,debug=false,"
    //        + "unsafe=true,probeDescPath=.,noServer=true,"
    //        + "script=/home/yiwei/btrace/gpu/KMeansGPU.class,"
    //        + "scriptOutputFile=%s");
    //conf.set("mapreduce.task.profile.maps","0");
    //conf.set("mapreduce.task.profile.reduces","0");
    //conf.setInt("mapreduce.job.jvm.numtasks", 1);
    //conf.setInt("mapreduce.map.combine.minspills", 9999);
    //conf.setBoolean("mapreduce.map.speculative", false);
    //conf.setBoolean("mapreduce.reduce.speculative", false);
    //conf.setInt("mapreduce.reduce.shuffle.parallelcopies", 1);

    conf.setFloat("mapreduce.reduce.input.buffer.percent", 1.0f);
    conf.setInt("mapreduce.reduce.merge.inmem.threshold ", 0);
    conf.setInt("mapreduce.task.io.sort.mb", 300);


    System.out.println("Value of K = " + K);
        
    kmeans(K, getConf());
    return 0;
  }

  public static void main(String[] argv) throws Exception {
    System.exit(ToolRunner.run(null, new KMeans(), argv));
  }
}
