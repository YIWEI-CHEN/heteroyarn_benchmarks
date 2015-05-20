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

import com.amd.aparapi.Kernel;

public class BlackScholes extends Configured implements Tool {

  public static String INPUT_DIR =  "/user/yiwei/black_scholes/input";
  public static String OUTPUT_DIR = "/user/yiwei/black_scholes/output";
  
  public static class BSMapper extends 
      Mapper<LongWritable, Text, FloatWritable, FloatWritable> {

    @Override
    public void run(Context context) 
        throws IOException, InterruptedException {
      setup(context);
      long aparapiConversionTime = 0L, aparapiExecutionTime = 0L,
          aparapiBufferWriteTime = 0L, aparapiKernelTime = 0L, aparapiBufferReadTime = 0L;

      try {
        List<Float> valuesList = new ArrayList<Float>();
	while(context.nextKeyValue()) {
	  float value = Float.valueOf(context.getCurrentValue().toString());
          valuesList.add(value);
	}

        final int size = valuesList.size();
        final float[] values = new float[size];
        final float[] puts = new float[size];
        final float[] calls = new float[size];
        for(int index = 0;index < size;index++) {
          values[index] = valuesList.get(index);
        }

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

//        if(isGpuMapper()) {
          final int splits = 4;
          final int _size = size / splits;
          float[] _values = new float[_size];
          float[] _calls, _puts;
          MyKernel kernel = new MyKernel();
          for(int i = 0;i < splits;i++) {
            for(int index = 0;index < _size;index++) {
              _values[index] = values[i * _size + index];
            }
            kernel.setValues(_values);
            kernel.execute(_size);
            _puts = kernel.getPuts();
            _calls = kernel.getCalls();
            for(int index = 0;index < _size;index++) {
              puts[i * _size + index] = _puts[index];
              calls[i * _size + index] = _calls[index];
            }
//            aparapiConversionTime += kernel.getConversionTime();
//            aparapiExecutionTime += kernel.getExecutionTime();
//            aparapiBufferWriteTime += kernel.getBufferHostToDeviceTime();
//            aparapiKernelTime += kernel.getKernelExecutionTime();
//            aparapiBufferReadTime += kernel.getBufferDeviceToHostTime();
//            updateAparapiCounters(context, aparapiConversionTime, aparapiExecutionTime,
//                aparapiBufferWriteTime, aparapiKernelTime, aparapiBufferReadTime);
          }
          kernel.dispose();
//        } else {
//          for(int index = 0;index < size;index++) {
//            float d1, d2;
//            float phiD1, phiD2;
//            float sigmaSqrtT;
//            float KexpMinusRT;
//
//            float two = 2.0f;
//            float inRand = values[index];
//            float S = S_LOWER_LIMIT * inRand + S_UPPER_LIMIT * (1.0f - inRand);
//            float K = K_LOWER_LIMIT * inRand + K_UPPER_LIMIT * (1.0f - inRand);
//            float T = T_LOWER_LIMIT * inRand + T_UPPER_LIMIT * (1.0f - inRand);
//            float R = R_LOWER_LIMIT * inRand + R_UPPER_LIMIT * (1.0f - inRand);
//            float sigmaVal = SIGMA_LOWER_LIMIT * inRand + 
//                SIGMA_UPPER_LIMIT * (1.0f - inRand);
//
//            sigmaSqrtT = sigmaVal * (float)Math.sqrt(T);
//            d1 = ((float)Math.log(S / K) + (R + sigmaVal * sigmaVal / two) * T)
//                / sigmaSqrtT;
//            d2 = d1 - sigmaSqrtT;
//            KexpMinusRT = K * (float)Math.exp(-R * T);
//
//            phiD1 = phi(d1);
//            phiD2 = phi(d2);
//            calls[index]  = S * phiD1 - KexpMinusRT * phiD2;
//
//            phiD1 = phi(-d1);
//            phiD2 = phi(-d2);
//            puts[index] = KexpMinusRT * phiD2 - S * phiD1;
//          }
//        }

        for(int index = 0;index < size;index++) {
          context.write(new FloatWritable(puts[index]), 
              new FloatWritable(calls[index]));
        }
      } finally {
	cleanup(context);
      }
    }

    private class MyKernel extends Kernel {
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

      private float[] _values;
      private float[] _calls;
      private float[] _puts;

      @Override
      public void run() {
        int gid = getGlobalId();

        float d1, d2;
        float phiD1, phiD2;
        float sigmaSqrtT;
        float KexpMinusRT;

        float two = 2.0f;
        float inRand = _values[gid];
        float S = S_LOWER_LIMIT * inRand + S_UPPER_LIMIT * (1.0f - inRand);
        float K = K_LOWER_LIMIT * inRand + K_UPPER_LIMIT * (1.0f - inRand);
        float T = T_LOWER_LIMIT * inRand + T_UPPER_LIMIT * (1.0f - inRand);
        float R = R_LOWER_LIMIT * inRand + R_UPPER_LIMIT * (1.0f - inRand);
        float sigmaVal = SIGMA_LOWER_LIMIT * inRand + 
            SIGMA_UPPER_LIMIT * (1.0f - inRand);

        sigmaSqrtT = sigmaVal * sqrt(T);
        d1 = (log(S / K) + (R + sigmaVal * sigmaVal / two) * T)
            / sigmaSqrtT;
        d2 = d1 - sigmaSqrtT;
        KexpMinusRT = K * exp(-R * T);

        phiD1 = _phi(d1);
        phiD2 = _phi(d2);
        _calls[gid]  = S * phiD1 - KexpMinusRT * phiD2;

        phiD1 = _phi(-d1);
        phiD2 = _phi(-d2);
        _puts[gid] = KexpMinusRT * phiD2 - S * phiD1;
      }

      public float _phi(float X) {
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

        float absX = abs(X);
        float t = one / (one + temp4 * absX);
        float y = one - oneBySqrt2pi * exp(-X * X / two) * t
            * (c1 + t * (c2 + t * (c3 + t * (c4 + t * c5))));
        float result = (X < zero) ? (one - y) : y;

        return result;
      }	

      public void setValues(float[] _values) {
        this._values = _values;
        this._calls = new float[_values.length];
        this._puts = new float[_values.length];
      }

      public float[] getCalls() {
        return _calls;
      }

      public float[] getPuts() {
        return _puts;
      }
    };

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

//    public void updateAparapiCounters(Context context,
//      long aparapiConversionTime, long aparapiExecutionTime,
//      long aparapiBufferWriteTime, long aparapiKernelTime, long aparapiBufferReadTime)
//      throws IOException, InterruptedException {
//      context.getCounter(TaskCounter.APARAPI_CONVERSION_MILLIS).
//          setValue(aparapiConversionTime);
//      context.getCounter(TaskCounter.APARAPI_EXECUTION_MILLIS).
//          setValue(aparapiExecutionTime);
//      context.getCounter(TaskCounter.APARAPI_BUFFER_WRITE_MILLIS).
//          setValue(aparapiBufferWriteTime);
//      context.getCounter(TaskCounter.APARAPI_KERNEL_MILLIS).
//          setValue(aparapiKernelTime);
//      context.getCounter(TaskCounter.APARAPI_BUFFER_READ_MILLIS).
//          setValue(aparapiBufferReadTime);
//    }
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
    job.setNumReduceTasks(50);

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
    blackScholes(getConf());
    return 0;
  }

  public static void main(String[] argv) throws Exception {
    System.exit(ToolRunner.run(null, new BlackScholes(), argv));
  }
}
