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
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class BlackScholes extends Configured implements Tool {

  public static String INPUT_DIR =  "/user/yiwei/black_scholes/input";
  public static String OUTPUT_DIR = "/user/yiwei/black_scholes/output";
  
  public static class BSMapper extends 
      Mapper<LongWritable, Text, FloatWritable, FloatWritable> {
    @Override
    protected void map(LongWritable key, Text value, Context context
            ) throws IOException, InterruptedException {
      final float S_LOWER_LIMIT = 10.0f;
      final float S_UPPER_LIMIT = 100.0f;
      final float K_LOWER_LIMIT = 10.0f;
      final float K_UPPER_LIMIT = 100.0f;
      final float T_LOWER_LIMIT = 1.0f;
      final float T_UPPER_LIMIT = 10.0f;
      final float R_LOWER_LIMIT = 0.01f;
      final float R_UPPER_LIMIT = 0.05f;
      final float SIGMA_LOWER_LIMIT = 0.01f;
      final float SIGMA_UPPER_LIMIT = 0.10f; 

      float _value = Float.valueOf(value.toString());
      float call, put;
      float d1, d2;
      float phiD1, phiD2;
      float sigmaSqrtT;
      float KexpMinusRT;

      float two = 2.0f;
      float inRand = _value;
      float S = S_LOWER_LIMIT * inRand + S_UPPER_LIMIT * (1.0f - inRand);
      float K = K_LOWER_LIMIT * inRand + K_UPPER_LIMIT * (1.0f - inRand);
      float T = T_LOWER_LIMIT * inRand + T_UPPER_LIMIT * (1.0f - inRand);
      float R = R_LOWER_LIMIT * inRand + R_UPPER_LIMIT * (1.0f - inRand);
      float sigmaVal = SIGMA_LOWER_LIMIT * inRand + SIGMA_UPPER_LIMIT * (1.0f - inRand);

      sigmaSqrtT = sigmaVal * (float)Math.sqrt(T);
      d1 = ((float)Math.log(S / K) + (R + sigmaVal * sigmaVal / two) * T) / sigmaSqrtT;
      d2 = d1 - sigmaSqrtT;
      KexpMinusRT = K * (float)Math.exp(-R * T);

      phiD1 = phi(d1);
      phiD2 = phi(d2);
      call  = S * phiD1 - KexpMinusRT * phiD2;

      phiD1 = phi(-d1);
      phiD2 = phi(-d2);
      put = KexpMinusRT * phiD2 - S * phiD1;
      context.write(new FloatWritable(put), new FloatWritable(call));
    }

    public float phi(float X) {
      final float c1 = 0.319381530f;
      final float c2 = -0.356563782f;
      final float c3 = 1.781477937f;
      final float c4 = -1.821255978f;
      final float c5 = 1.330274429f;
      final float zero = 0.0f;
      final float one = 1.0f;
      final float two = 2.0f;
      final float temp4 = 0.2316419f;
      final float oneBySqrt2pi = 0.398942280f;

      float absX = Math.abs(X);
      float t = one / (one + temp4 * absX);
      float y = one - oneBySqrt2pi * (float)Math.exp(-X * X / two) * t
          * (c1 + t * (c2 + t * (c3 + t * (c4 + t * c5))));
      float result = (X < zero) ? (one - y) : y;

      return result;
    }	

    int getTaskID(Context context)
		throws IOException, InterruptedException {
      String attempID = context.getTaskAttemptID().getTaskID().toString();
      String[] parts = attempID.split("_");
      int taskID = Integer.valueOf(parts[4]);

      return taskID;
    }
  }

  public static class BSReducer extends 
      Reducer<FloatWritable, FloatWritable, FloatWritable, FloatWritable> {
    
    public void reduce(FloatWritable put,
        Iterable<FloatWritable> calls, Context context)
        throws IOException, InterruptedException {
      for(FloatWritable call : calls) {
        context.write(put, call);
      }
    }
  }

  public static void blackScholes(Configuration conf
      ) throws IOException, ClassNotFoundException, InterruptedException {
    // Job job = new Job(conf) is deprecated
    Job job = Job.getInstance(conf);
    job.setJobName(BlackScholes.class.getSimpleName());
    job.setJarByClass(BlackScholes.class);

    job.setInputFormatClass(TextInputFormat.class);

    job.setOutputKeyClass(FloatWritable.class);
    job.setOutputValueClass(FloatWritable.class);
    job.setOutputFormatClass(TextOutputFormat.class);

    job.setMapperClass(BSMapper.class);

    job.setReducerClass(BSReducer.class);
    job.setNumReduceTasks(1);

    job.setSpeculativeExecution(false);

    final Path inDir = new Path(INPUT_DIR);
    final Path outDir = new Path(OUTPUT_DIR);
    FileInputFormat.setInputPaths(job, inDir);
    FileOutputFormat.setOutputPath(job, outDir);

    final FileSystem fs = FileSystem.get(conf);

    try {
//      System.out.println("Starting Job");
//      final long startTime = System.currentTimeMillis();
      job.waitForCompletion(true);
//      final double duration = (System.currentTimeMillis() - startTime)/1000.0;
//      System.out.println("Job Finished in " + duration + " seconds");
    } finally {
      fs.delete(outDir, true);
    }
  }

  public int run(String[] args) throws Exception {
    Configuration conf = getConf();
    conf.setBoolean("mapreduce.task.profile", true);
    conf.set("mapreduce.task.profile.params", "-javaagent:"
            + "/home/yiwei/btrace/btrace-agent.jar="
            + "dumpClasses=false,debug=false,"
            + "unsafe=true,probeDescPath=.,noServer=true,"
            + "script=/home/yiwei/btrace/HadoopBTrace2.class,"
            + "scriptOutputFile=%s");
    conf.set("mapreduce.task.profile.maps","0");
    conf.set("mapreduce.task.profile.reduces","0");
    conf.setInt("mapreduce.job.jvm.numtasks", 1);
    conf.setInt("mapreduce.map.combine.minspills", 9999);
    conf.setInt("mapreduce.reduce.shuffle.parallelcopies", 1);
    conf.setFloat("mapreduce.reduce.input.buffer.percent", 0f);
    conf.setBoolean("mapreduce.map.speculative", false);
    conf.setBoolean("mapreduce.reduce.speculative", false);
    blackScholes(conf);
    return 0;
  }

  public static void main(String[] argv) throws Exception {
    System.exit(ToolRunner.run(null, new BlackScholes(), argv));
  }
}
