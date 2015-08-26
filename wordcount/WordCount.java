/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class WordCount {

  public static class TokenizerMapper 
       extends Mapper<Object, Text, Text, IntWritable>{
    
    private final static IntWritable one = new IntWritable(1);
    private Text word = new Text();
      
    public void map(Object key, Text value, Context context
                    ) throws IOException, InterruptedException {
      StringTokenizer itr = new StringTokenizer(value.toString());
      while (itr.hasMoreTokens()) {
        word.set(itr.nextToken());
        context.write(word, one);
      }
    }
  }
  
  public static class IntSumReducer 
       extends Reducer<Text,IntWritable,Text,IntWritable> {
    private IntWritable result = new IntWritable();

    public void reduce(Text key, Iterable<IntWritable> values, 
                       Context context
                       ) throws IOException, InterruptedException {
      int sum = 0;
      for (IntWritable val : values) {
        sum += val.get();
      }
      result.set(sum);
      context.write(key, result);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
//    conf.setBoolean("mapreduce.task.profile", true);
//    conf.set("mapreduce.task.profile.maps","0");
//    conf.set("mapreduce.task.profile.reduces","0");
    // profile using HPROF
//    conf.set("mapreduce.task.profile.params","-agentlib:hprof=cpu=samples,depth=20,force=n,thread=y,verbose=n,file=%s");
//    conf.set("mapreduce.task.profile.params","-agentlib:hprof=heap=sites,depth=6,force=n,thread=y,verbose=n,file=%s");
    // profile using BTrace
//    conf.set("mapred.task.profile.params", "-javaagent:"
//            + "/home/yiwei/btrace/btrace-agent.jar="
//            + "dumpClasses=false,debug=false,"
//            + "unsafe=true,probeDescPath=.,noServer=true,"
//            + "script=/home/yiwei/btrace/HadoopBTrace2.class,"
//            + "scriptOutputFile=%s");
//    conf.setInt("mapreduce.job.jvm.numtasks", 1);
//    conf.setInt("mapreduce.map.combine.minspills", 9999);
//    conf.setInt("mapreduce.reduce.shuffle.parallelcopies", 1);
//    conf.setFloat("mapreduce.reduce.input.buffer.percent", 0f);
//    conf.setBoolean("mapreduce.map.speculative", false);
//    conf.setBoolean("mapreduce.reduce.speculative", false);
    if (otherArgs.length < 3) {
      System.err.println("Usage: wordcount <in> [<in>...] <out> <numOfReduce>");
      System.exit(2);
    }
    final int totalReduces = Integer.parseInt(otherArgs[otherArgs.length-1]);
    Job job = Job.getInstance(conf);
    job.setJobName(WordCount.class.getSimpleName());
    job.setJarByClass(WordCount.class);
    job.setMapperClass(TokenizerMapper.class);
    job.setCombinerClass(IntSumReducer.class);
    job.setReducerClass(IntSumReducer.class);
    job.setOutputKeyClass(Text.class);
    job.setOutputValueClass(IntWritable.class);
    job.setNumReduceTasks(totalReduces);
    for (int i = 0; i < otherArgs.length - 2; ++i) {
      FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
    }
    FileOutputFormat.setOutputPath(job,
      new Path(otherArgs[otherArgs.length - 2]));

    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
