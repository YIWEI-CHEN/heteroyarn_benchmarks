import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.amd.aparapi.Kernel;
import com.amd.aparapi.ProfileInfo;
public class QuasiMonteCarlo extends Configured implements Tool {
  static private final String TMP_DIR_PREFIX = QuasiMonteCarlo.class.getSimpleName();

  public static class QmcMapper extends 
      Mapper<LongWritable, LongWritable, BooleanWritable, LongWritable> {

    private Kernel.EXECUTION_MODE executionMode;

    public QmcMapper() {
      executionMode = Kernel.EXECUTION_MODE.GPU;
    }

    @Override
    public void run(Context context)
		    throws IOException, InterruptedException {
      setup(context);
      try {
	final int baseX = 2, baseY = 3;
	final int maxIterX = 63, maxIterY = 40;
	final int offset, size;
	if(context.nextKeyValue()) {
	  offset = (int)(context.getCurrentKey().get());
	  size = (int)(context.getCurrentValue().get());
	} else {
	  offset = 0;
	  size = 1;
	}
	final boolean[] isInside = new boolean[(int)size];

        if(isGpuMapper()) {
	  Kernel kernel = new Kernel() {
	    @Override public void run() {
	      int gid = getGlobalId();
	      double x = 0.0, y = 0.0;
	      double f =  1.0 / baseX;
	      int index = offset + gid + 1;
	      int iter = 0;
	      while(index > 0 || iter < maxIterX) {
		x  = x + f * (index % baseX);
		index = index / baseX;
		f = f / baseX;
		iter = iter + 1;
	      }

	      f = 1.0 / baseY;
	      index = offset + gid + 1;
	      iter = 0;
	      while(index > 0 || iter < maxIterY) {
		y  = y + f * (index % baseY);
		index = index / baseY;
		f = f / baseY;
		iter = iter + 1;
	      }
	      
	      double _x = x - 0.5;
	      double _y = y - 0.5;
	      isInside[gid] =  (_x * _x + _y * _y) <= 0.25;
	    }
	  };
	  executionMode = Kernel.EXECUTION_MODE.GPU;
	  kernel.setExecutionMode(executionMode);
	  kernel.execute(size);
	  if(kernel.getExecutionMode() == Kernel.EXECUTION_MODE.GPU) {
	    System.out.println("THIS IS GPU MODE OF APARAPI");
	  } else {
	    System.out.println("THIS IS JPT MODE OF APARAPI");
	  }
	  updateAparapiCounters(context, kernel.getConversionTime(), kernel.getExecutionTime(),
	  kernel.getBufferHostToDeviceTime(), kernel.getKernelExecutionTime(), kernel.getBufferDeviceToHostTime());
	  kernel.dispose();
	} else {
	  for(int i = 0;i < size;i++) {
	    double x = 0.0, y = 0.0;
	    double f =  1.0 / baseX;
	    int index = offset + i + 1;
	    int iter = 0;
	    while(index > 0 || iter < maxIterX) {
	      x  = x + f * (index % baseX);
	      index = index / baseX;
	      f = f / baseX;
	      iter = iter + 1;
	    }

	    f = 1.0 / baseY;
	    index = offset + i + 1;
	    iter = 0;
	    while(index > 0 || iter < maxIterY) {
	      y  = y + f * (index % baseY);
	      index = index / baseY;
	      f = f / baseY;
	      iter = iter + 1;
	    }

	    double _x = x - 0.5;
	    double _y = y - 0.5;
	    isInside[i] = (_x * _x + _y * _y) <= 0.25;
	  }
	  System.out.println("THIS IS RUNNING ON CPU");
          updateAparapiCounters(context, 0, 0, 0, 0, 0);
	}

	int numInside = 0, numOutside = 0;
	for(int index = 0;index < size;index++) {
	  if(isInside[index]) {
	    numInside++;
	  } else {
	    numOutside++;
	  }
	}
	
	context.write(new BooleanWritable(true), new LongWritable(numInside));
	context.write(new BooleanWritable(false), new LongWritable(numOutside));

      } finally {
	cleanup(context);
      }
    }

    public void updateAparapiCounters(Context context,
				      long aparapiConversionTime, long aparapiExecutionTime,
				      long aparapiBufferWriteTime, long aparapiKernelTime, long aparapiBufferReadTime)
				      throws IOException, InterruptedException {
      context.getCounter(TaskCounter.APARAPI_CONVERSION_MILLIS).setValue(aparapiConversionTime);
      context.getCounter(TaskCounter.APARAPI_EXECUTION_MILLIS).setValue(aparapiExecutionTime);
      context.getCounter(TaskCounter.APARAPI_BUFFER_WRITE_MILLIS).setValue(aparapiBufferWriteTime);
      context.getCounter(TaskCounter.APARAPI_KERNEL_MILLIS).setValue(aparapiKernelTime);
      context.getCounter(TaskCounter.APARAPI_BUFFER_READ_MILLIS).setValue(aparapiBufferReadTime);
    }

    int getTaskID(Context context)
		throws IOException, InterruptedException {
      String attempID = context.getTaskAttemptID().getTaskID().toString();
      String[] parts = attempID.split("_");
      int taskID = Integer.valueOf(parts[4]);

      return taskID;
    }
  }

  public static class QmcReducer extends 
      Reducer<BooleanWritable, LongWritable, WritableComparable<?>, Writable> {
    
    private long numInside = 0;
    private long numOutside = 0;
      
    public void reduce(BooleanWritable isInside,
        Iterable<LongWritable> values, Context context)
        throws IOException, InterruptedException {
      if (isInside.get()) {
        for (LongWritable val : values) {
          numInside += val.get();
        }
      } else {
        for (LongWritable val : values) {
          numOutside += val.get();
        }
      }
    }

    @Override
    public void cleanup(Context context) throws IOException {
      //write output to a file
      Configuration conf = context.getConfiguration();
      Path outDir = new Path(conf.get(FileOutputFormat.OUTDIR));
      Path outFile = new Path(outDir, "reduce-out");
      FileSystem fileSys = FileSystem.get(conf);
      SequenceFile.Writer writer = SequenceFile.createWriter(fileSys, conf,
          outFile, LongWritable.class, LongWritable.class, 
          CompressionType.NONE);
      writer.append(new LongWritable(numInside), new LongWritable(numOutside));
      writer.close();
    }
  }

  public static BigDecimal estimatePi(int numMaps, long numPoints,
      Path tmpDir, Configuration conf
      ) throws IOException, ClassNotFoundException, InterruptedException {
    Job job = new Job(conf);
    //setup job conf
    job.setJobName(QuasiMonteCarlo.class.getSimpleName());
    job.setJarByClass(QuasiMonteCarlo.class);

    job.setInputFormatClass(SequenceFileInputFormat.class);

    job.setOutputKeyClass(BooleanWritable.class);
    job.setOutputValueClass(LongWritable.class);
    job.setOutputFormatClass(SequenceFileOutputFormat.class);

    job.setMapperClass(QmcMapper.class);

    job.setReducerClass(QmcReducer.class);
    job.setNumReduceTasks(1);

    // turn off speculative execution, because DFS doesn't handle
    // multiple writers to the same file.
    job.setSpeculativeExecution(false);

    //setup input/output directories
    final Path inDir = new Path(tmpDir, "in");
    final Path outDir = new Path(tmpDir, "out");
    FileInputFormat.setInputPaths(job, inDir);
    FileOutputFormat.setOutputPath(job, outDir);

    final FileSystem fs = FileSystem.get(conf);
    if (fs.exists(tmpDir)) {
      throw new IOException("Tmp directory " + fs.makeQualified(tmpDir)
          + " already exists.  Please remove it first.");
    }
    if (!fs.mkdirs(inDir)) {
      throw new IOException("Cannot create input directory " + inDir);
    }

    try {
      //generate an input file for each map task
      for(int i = 0; i < numMaps; ++i) {
        final Path file = new Path(inDir, "part" + i);
        System.out.println("file input = " + file);
        final SequenceFile.Writer writer = SequenceFile.createWriter(
          fs, conf, file,
          LongWritable.class, LongWritable.class, CompressionType.NONE);
	final LongWritable offset = new LongWritable(i * numPoints);
	final LongWritable size = new LongWritable(numPoints);
        try {
	  writer.append(offset, size);
        } finally {
          writer.close();
        }
        System.out.println("Wrote input for Map #"+i);
      }
  
      //start a map/reduce job
      System.out.println("Starting Job");
      final long startTime = System.currentTimeMillis();
      job.waitForCompletion(true);
      final double duration = (System.currentTimeMillis() - startTime)/1000.0;
      System.out.println("Job Finished in " + duration + " seconds");

      //read outputs
      Path inFile = new Path(outDir, "reduce-out");
      LongWritable numInside = new LongWritable();
      LongWritable numOutside = new LongWritable();
      SequenceFile.Reader reader = new SequenceFile.Reader(fs, inFile, conf);
      try {
        reader.next(numInside, numOutside);
      } finally {
        reader.close();
      }

      //compute estimated value
      final BigDecimal numTotal
          = BigDecimal.valueOf(numMaps).multiply(BigDecimal.valueOf(numPoints));
      return BigDecimal.valueOf(4).setScale(20)
          .multiply(BigDecimal.valueOf(numInside.get()))
          .divide(numTotal, RoundingMode.HALF_UP);
    } finally {
      fs.delete(tmpDir, true);
    }
  }

  public int run(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.println("Usage: "+getClass().getName()+" <nMaps> <nSamples>");
      ToolRunner.printGenericCommandUsage(System.err);
      return 2;
    }
    
    final int nMaps = Integer.parseInt(args[0]);
    final long nSamples = Long.parseLong(args[1]);
    long now = System.currentTimeMillis();
    int rand = new Random().nextInt(Integer.MAX_VALUE);
    final Path tmpDir = new Path(TMP_DIR_PREFIX + "_" + now + "_" + rand);
        
    System.out.println("Number of Maps  = " + nMaps);
    System.out.println("Samples per Map = " + nSamples);
        
    System.out.println("Estimated value of Pi is "
        + estimatePi(nMaps, nSamples, tmpDir, getConf()));
    return 0;
  }

  public static void main(String[] argv) throws Exception {
    System.exit(ToolRunner.run(null, new QuasiMonteCarlo(), argv));
  }
}
