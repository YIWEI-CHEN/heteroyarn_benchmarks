#/bin/sh/
CUR_DIR=`pwd`
[ -d ${CUR_DIR}/log ] || mkdir ${CUR_DIR}/log
TIMESTAMP=$(date +%-d-%-m-%Y-%T)

hdfs dfs -rm -r output
hadoop jar ${HADOOP_HOME}/share/hadoop/mapreduce/hadoop-mapreduce-examples-2.6.0.jar grep input output 'dfs[a-z.]+' 2>&1 | tee "${CUR_DIR}/log/grep.${TIMESTAMP}.log"

[ -d ${CUR_DIR}/output ] && rm -r ${CUR_DIR}/output
hdfs dfs -cat output/*
