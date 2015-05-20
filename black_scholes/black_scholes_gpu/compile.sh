#/bin/sh/
CUR_DIR=`pwd`

#MY_HADOOP_HOME=/home/heterohadoop/hadoop-2.2.0
MY_HADOOP_HOME=/usr/local/hadoop
MY_CLASSPATH=$MY_HADOOP_HOME/share/hadoop/common/hadoop-common-2.6.0.jar:$MY_HADOOP_HOME/share/hadoop/common/lib/hadoop-annotations-2.6.0.jar:$MY_HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-client-core-2.6.0.jar:$MY_HADOOP_HOME/share/hadoop/common/lib/commons-cli-1.2.jar:$MY_HADOOP_HOME/share/hadoop/common/lib/aparapi.jar

mkdir -p $CUR_DIR/classes
javac -g -classpath $MY_CLASSPATH -d $CUR_DIR/classes $CUR_DIR/BlackScholes.java
jar -cvf black_scholes.jar -C $CUR_DIR/classes . 
