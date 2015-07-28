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

import csie.pas.aparapi.Kernel;
import com.amd.aparapi.Aparapi;

public class KMeans extends Configured implements Tool {

  public static String INPUT_DIR     = "kmeans/input";
  public static String OUTPUT_DIR    = "kmeans/output";
  public static String CENTROIDS_DIR = "kmeans/centroids";
  
  public static class KmMapper extends 
      Mapper<LongWritable, Text, LongWritable, Text> {

    private int K;
    private double[] centroidsX;
    private double[] centroidsY;
    private double[] centroidsZ;
    private double[] centroidsH;
    private double[] centroidsW;

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
        centroidsH = new double[K];
        centroidsW = new double[K];
        int count = 0;
        String line;

        while ((count < K) && ((line = cacheReader.readLine()) != null)) {
          String[] xyz = line.split(" ");
          centroidsX[count] = Double.valueOf(xyz[0]);
          centroidsY[count] = Double.valueOf(xyz[1]);
          centroidsZ[count] = Double.valueOf(xyz[2]);
          centroidsH[count] = Double.valueOf(xyz[3]);
          centroidsW[count] = Double.valueOf(xyz[4]);
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
      long aparapiConversionTime = 0L, aparapiExecutionTime = 0L,
          aparapiBufferWriteTime = 0L, aparapiKernelTime = 0L, aparapiBufferReadTime = 0L;
      long startTime = 0;
      long endTime = 0;
      String type="";

      try {
        final int _K = K;
        final double maxDistance = Double.MAX_VALUE;
        final double[] _centroidsX = new double[_K];
        final double[] _centroidsY = new double[_K];
        final double[] _centroidsZ = new double[_K];
        final double[] _centroidsH = new double[_K];
        final double[] _centroidsW = new double[_K];
        final double[] pointsX;
        final double[] pointsY;
        final double[] pointsZ;
        final double[] pointsH;
        final double[] pointsW;
        List<Double> pointsListX = new ArrayList<Double>();
        List<Double> pointsListY = new ArrayList<Double>();
        List<Double> pointsListZ = new ArrayList<Double>();
        List<Double> pointsListH = new ArrayList<Double>();
        List<Double> pointsListW = new ArrayList<Double>();
        final int[] nearestCentroids;

        System.arraycopy(centroidsX, 0, _centroidsX, 0, _K);
        System.arraycopy(centroidsY, 0, _centroidsY, 0, _K);
        System.arraycopy(centroidsZ, 0, _centroidsZ, 0, _K);
        System.arraycopy(centroidsH, 0, _centroidsH, 0, _K);
        System.arraycopy(centroidsW, 0, _centroidsW, 0, _K);
	while(context.nextKeyValue()) {
	  String[] xyz = context.getCurrentValue().toString().split(" ");
          pointsListX.add(Double.valueOf(xyz[0]));
          pointsListY.add(Double.valueOf(xyz[1]));
          pointsListZ.add(Double.valueOf(xyz[2]));
          pointsListH.add(Double.valueOf(xyz[3]));
          pointsListW.add(Double.valueOf(xyz[4]));
	}

        final int size = pointsListX.size();
        pointsX = new double[size];
        pointsY = new double[size];
        pointsZ = new double[size];
        pointsH = new double[size];
        pointsW = new double[size];
        nearestCentroids = new int[size];
        for(int index = 0;index < size;index++) {
          pointsX[index] = pointsListX.get(index);
          pointsY[index] = pointsListY.get(index);
          pointsZ[index] = pointsListZ.get(index);
          pointsH[index] = pointsListH.get(index);
          pointsW[index] = pointsListW.get(index);
        }

        if(isGPUMapper()) {
          Kernel kernel = new Kernel() {
            @Override public void run() {
              int gid = getGlobalId();
              double minDistance = maxDistance;
              int nearestCentroid = -1;
              for(int i = 0;i < _K;i++) {
                double diffX = pointsX[gid] - _centroidsX[i];
                double diffY = pointsY[gid] - _centroidsY[i];
                double diffZ = pointsZ[gid] - _centroidsZ[i];
                double diffH = pointsH[gid] - _centroidsH[i];
                double diffW = pointsW[gid] - _centroidsW[i];
                double distance = diffX * diffX + diffY * diffY + diffZ * diffZ +diffH * diffH+diffW * diffW;
                if(distance < minDistance) {
                  minDistance = distance;
                  nearestCentroid = i;
                }
              }
              nearestCentroids[gid] = nearestCentroid;
            }
          };
          type="GPU";
          startTime = System.currentTimeMillis();
          kernel.execute(size);
         // aparapiConversionTime += kernel.getConversionTime();
         // aparapiExecutionTime += kernel.getExecutionTime();
         // aparapiBufferWriteTime += kernel.getBufferHostToDeviceTime();
         // aparapiKernelTime += kernel.getKernelExecutionTime();
         // aparapiBufferReadTime += kernel.getBufferDeviceToHostTime();
          kernel.dispose();
          endTime = System.currentTimeMillis();
         System.out.println("kernel mode\t"+kernel.getExecutionMode());
         // updateAparapiCounters(context, aparapiConversionTime, aparapiExecutionTime,
         //     aparapiBufferWriteTime, aparapiKernelTime, aparapiBufferReadTime);
        } else if(isHSAMapper()){
            type="HSA";
            startTime = System.currentTimeMillis();
            Aparapi.range(size).parallel().forEach(gid ->{
                double minDistance = maxDistance;
                int nearestCentroid = -1;
                for(int i = 0;i < _K;i++) {
                  double diffX = pointsX[gid] - _centroidsX[i];
                  double diffY = pointsY[gid] - _centroidsY[i];
                  double diffZ = pointsZ[gid] - _centroidsZ[i];
                  double diffH = pointsH[gid] - _centroidsH[i];
                  double diffW = pointsW[gid] - _centroidsW[i];
                  double distance = diffX * diffX + diffY * diffY + diffZ * diffZ +diffH * diffH+diffW * diffW;
                  if(distance < minDistance) {
                    minDistance = distance;
                    nearestCentroid = i;
                  }
                }
                nearestCentroids[gid] = nearestCentroid;
            });
          endTime = System.currentTimeMillis();
        }else{
          type="CPU";
          startTime = System.currentTimeMillis();
          for(int index = 0;index < size;index++) {
            double minDistance = maxDistance;
            int nearestCentroid = -1;
            for(int i = 0;i < _K;i++) {
              double diffX = pointsX[index] - centroidsX[i];
              double diffY = pointsY[index] - centroidsY[i];
              double diffZ = pointsZ[index] - centroidsZ[i];
              double diffH = pointsH[index] - _centroidsH[i];
              double diffW = pointsW[index] - _centroidsW[i];
              double distance = diffX * diffX + diffY * diffY + diffZ * diffZ +diffH * diffH+diffW * diffW;
              if(distance < minDistance) {
                minDistance = distance;
                nearestCentroid = i;
              }
            }
            nearestCentroids[index] = nearestCentroid;
          }
          endTime = System.currentTimeMillis();
        }

        for(int index = 0;index < size;index++) {
          String point = pointsX[index] + " "
              + pointsY[index] + " " + pointsZ[index] + " " + pointsH[index] + " " + pointsW[index];
          context.write(new LongWritable(nearestCentroids[index]), 
              new Text(point));
        }
        
      } finally {
          System.out.println("MAP in Mapper/MAP/" + startTime + "/" + endTime + "/" + (endTime-startTime) + "/" + type );
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
      //context.getCounter(TaskCounter.APARAPI_CONVERSION_MILLIS).setValue(aparapiConversionTime);
      //context.getCounter(TaskCounter.APARAPI_EXECUTION_MILLIS).setValue(aparapiExecutionTime);
      //context.getCounter(TaskCounter.APARAPI_BUFFER_WRITE_MILLIS).setValue(aparapiBufferWriteTime);
      //context.getCounter(TaskCounter.APARAPI_KERNEL_MILLIS).setValue(aparapiKernelTime);
      //context.getCounter(TaskCounter.APARAPI_BUFFER_READ_MILLIS).setValue(aparapiBufferReadTime);
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
      double newCentroidW = 0.0;
      double newCentroidH = 0.0;
      int numElements = 0;
      List<Text> pointsList = new ArrayList<Text>();

      for(Text point : points) {
        pointsList.add(new Text(point.toString()));
        String[] xyz = point.toString().split(" ");
        newCentroidX += Double.valueOf(xyz[0]);
        newCentroidY += Double.valueOf(xyz[1]);
        newCentroidZ += Double.valueOf(xyz[2]);
        newCentroidH += Double.valueOf(xyz[3]);
        newCentroidW += Double.valueOf(xyz[4]);
        numElements++;
      }
      newCentroidX /= numElements;
      newCentroidY /= numElements;
      newCentroidZ /= numElements;
      newCentroidH /= numElements;
      newCentroidW /= numElements;
      
      context.write(NullWritable.get(), 
          new Text(newCentroidX + " " + newCentroidY + " " + newCentroidZ + " " + newCentroidH + " " + newCentroidW));
      for(Text point : pointsList) {
        context.write(NullWritable.get(), point); 
      }
    }
  }

  public static void kmeans(int K, Configuration conf
      ) throws IOException, ClassNotFoundException, InterruptedException {
    //Job job = new Job(conf);
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
    job.setNumReduceTasks(40);

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
