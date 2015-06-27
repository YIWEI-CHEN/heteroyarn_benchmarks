#/bin/sh/
CUR_DIR=`pwd`

MY_HADOOP_HOME=/usr/local/hadoop
MY_CLASSPATH=$MY_HADOOP_HOME/share/hadoop/common/hadoop-common-2.2.0.jar:$MY_HADOOP_HOME/share/hadoop/mapreduce/hadoop-mapreduce-client-core-2.2.0.jar:$MY_HADOOP_HOME/share/hadoop/common/lib/commons-cli-1.2.jar:$MY_HADOOP_HOME/share/hadoop/common/lib/hadoop-annotations-2.2.0.jar


mkdir -p $CUR_DIR/classes
javac -Xlint -classpath $MY_CLASSPATH -d $CUR_DIR/classes $CUR_DIR/KMeans.java
jar -cvf kmeans.jar -C $CUR_DIR/classes . 
